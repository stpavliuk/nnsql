package nnsql.query.builder;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.*;
import nnsql.query.ir.Return.AttributeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class IRBuilder {
    private final SchemaRegistry schema;
    private final AtomicInteger nodeIdCounter = new AtomicInteger(0);

    private static final Set<String> AGGREGATE_FUNCTIONS =
        Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

    public IRBuilder(SchemaRegistry schema) {
        this.schema = schema;
    }

    public IRNode build(PlainSelect select) {
        var pipeline = IRPipeline.start();

        var relations = extractRelations(select);
        pipeline = pipeline.product(relations);

        if (select.getWhere() != null) {
            var attributes = AttributeResolver.collectFrom(pipeline.build());
            var condition = toCondition(select.getWhere());
            var qualifiedCondition = AttributeResolver.qualifyCondition(condition, attributes);
            pipeline = pipeline.filter(qualifiedCondition, attributes);
        }

        boolean hasAggregates = hasAggregatesInSelect(select);
        boolean hasGroupBy = select.getGroupBy() != null;

        if (hasGroupBy || hasAggregates) {
            pipeline = addGroupBy(select, pipeline);
        }

        if (select.getHaving() != null) {
            var attributes = AttributeResolver.collectFrom(pipeline.build());
            var condition = toCondition(select.getHaving());
            var qualifiedCondition = AttributeResolver.qualifyCondition(condition, attributes);
            pipeline = pipeline.aggFilter(qualifiedCondition, attributes);
        }

        if (hasGroupBy || hasAggregates) {
            pipeline = buildReturnForGroupBy(select, pipeline);
        } else {
            pipeline = buildReturnForNonGroupBy(select, pipeline);
        }

        if (select.getDistinct() != null) {
            var attributes = AttributeResolver.collectFrom(pipeline.build());
            pipeline = pipeline.duplElim(attributes);
        }

        return pipeline.build();
    }

    private List<Relation> extractRelations(PlainSelect select) {
        var relations = new ArrayList<Relation>();
        relations.add(toRelation(select.getFromItem()));

        if (select.getJoins() != null) {
            for (var join : select.getJoins()) {
                relations.add(toRelation(join.getFromItem()));
            }
        }

        return relations;
    }

    private Relation toRelation(FromItem from) {
        return switch (from) {
            case Table t -> {
                var tableName = t.getName();
                var alias = t.getAlias() != null ? t.getAlias().getName() : tableName;
                var attributes = schema.getAttributes(tableName);

                if (attributes.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Table '%s' not found in schema. Please register the table schema first."
                            .formatted(tableName));
                }

                yield new Relation.Table(tableName, alias, attributes);
            }
            case ParenthesedSelect ps -> {
                var alias = ps.getAlias() != null
                    ? ps.getAlias().getName()
                    : "subq" + nodeIdCounter.getAndIncrement();
                var subqueryIR = build((PlainSelect) ps.getSelect());
                var attributes = AttributeResolver.collectFrom(subqueryIR);
                yield new Relation.Subquery(alias, subqueryIR, attributes);
            }
            default -> throw new UnsupportedOperationException(
                "Unsupported FROM item: " + from.getClass().getSimpleName());
        };
    }

    IRExpression toExpression(Expression expr) {
        return switch (expr) {
            case Column col -> toColumnRef(col);
            case LongValue lv -> IRExpression.number((double) lv.getValue());
            case DoubleValue dv -> IRExpression.number(dv.getValue());
            case StringValue sv -> IRExpression.string(sv.getValue());
            case NullValue _ -> IRExpression.nullValue();
            case Function fn when isAggregate(fn) -> toAggregate(fn, null);
            case ParenthesedExpressionList<?> p -> toExpression(p.getFirst());
            case ParenthesedSelect ps -> toScalarSubquery(ps);
            default -> throw new UnsupportedOperationException(
                "Unsupported expression: " + expr.getClass().getSimpleName());
        };
    }

    private IRExpression.ColumnRef toColumnRef(Column col) {
        var table = col.getTable();
        var name = (table != null && table.getName() != null)
            ? table.getName() + "_" + col.getColumnName()
            : col.getColumnName();
        return new IRExpression.ColumnRef(name);
    }

    private boolean isAggregate(Function fn) {
        return fn.getName() != null && AGGREGATE_FUNCTIONS.contains(fn.getName().toUpperCase());
    }

    private IRExpression.Aggregate toAggregate(Function fn, String alias) {
        var functionName = fn.getName().toUpperCase();
        var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
            ? toExpression(fn.getParameters().getFirst())
            : new IRExpression.ColumnRef("*");
        return new IRExpression.Aggregate(functionName, argument, alias);
    }

    private IRExpression.ScalarSubquery toScalarSubquery(ParenthesedSelect ps) {
        var subqueryIR = build((PlainSelect) ps.getSelect());
        var pipeline = new ArrayList<IRNode>();
        pipeline.addFirst(subqueryIR);
        return new IRExpression.ScalarSubquery(pipeline);
    }

    Condition toCondition(Expression expr) {
        return switch (expr) {
            case AndExpression and -> {
                var operands = new ArrayList<Condition>();
                flattenAnd(and, operands);
                yield new Condition.And(operands);
            }
            case OrExpression or -> {
                var operands = new ArrayList<Condition>();
                flattenOr(or, operands);
                yield new Condition.Or(operands);
            }
            case NotExpression not -> Condition.not(toCondition(not.getExpression()));
            case IsNullExpression isn -> {
                var col = toExpression(isn.getLeftExpression());
                yield switch (col) {
                    case IRExpression.ColumnRef(var name) ->
                        new Condition.IsNull(name, isn.isNot());
                    default -> throw new IllegalArgumentException("IS NULL only on columns");
                };
            }
            case EqualsTo eq -> toComparison(eq, "=");
            case NotEqualsTo neq -> toComparison(neq, "!=");
            case GreaterThan gt -> toComparison(gt, ">");
            case MinorThan lt -> toComparison(lt, "<");
            case GreaterThanEquals gte -> toComparison(gte, ">=");
            case MinorThanEquals lte -> toComparison(lte, "<=");
            case ParenthesedExpressionList<?> p -> toCondition(p.getFirst());
            default -> throw new UnsupportedOperationException(
                "Unsupported condition: " + expr.getClass().getSimpleName());
        };
    }

    private void flattenAnd(Expression expr, List<Condition> operands) {
        if (expr instanceof AndExpression and) {
            flattenAnd(and.getLeftExpression(), operands);
            flattenAnd(and.getRightExpression(), operands);
        } else {
            operands.add(toCondition(expr));
        }
    }

    private void flattenOr(Expression expr, List<Condition> operands) {
        if (expr instanceof OrExpression or) {
            flattenOr(or.getLeftExpression(), operands);
            flattenOr(or.getRightExpression(), operands);
        } else {
            operands.add(toCondition(expr));
        }
    }

    private Condition toComparison(ComparisonOperator op, String operator) {
        var left = toExpression(op.getLeftExpression());
        var right = toExpression(op.getRightExpression());
        return Condition.compare(left, operator, right);
    }

    private boolean hasAggregatesInSelect(PlainSelect select) {
        for (var item : select.getSelectItems()) {
            if (item.getExpression() instanceof Function fn && isAggregate(fn)) {
                return true;
            }
        }
        return false;
    }

    private String extractAttributeName(SelectItem<?> item) {
        var expr = item.getExpression();
        return switch (toExpression(expr)) {
            case IRExpression.ColumnRef col -> col.columnName();
            case IRExpression.Literal _ ->
                throw new IllegalArgumentException("Cannot use literal in SELECT without alias");
            case IRExpression.Arithmetic _ ->
                throw new IllegalArgumentException("Cannot use arithmetic expression in SELECT without alias");
            case IRExpression.Aggregate agg -> agg.alias();
            case IRExpression.ScalarSubquery _ ->
                throw new IllegalArgumentException("Cannot use scalar subquery in SELECT without alias");
        };
    }

    private IRPipeline buildReturnForNonGroupBy(PlainSelect select, IRPipeline pipeline) {
        var selectItems = select.getSelectItems();

        if (selectItems.size() == 1 && selectItems.getFirst().getExpression() instanceof AllColumns) {
            return pipeline.returnAll();
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectedAttrs = selectItems.stream()
            .filter(item -> !(item.getExpression() instanceof AllColumns))
            .map(item -> buildAttributeRef(item, availableAttrs))
            .toList();

        return pipeline.returnSelected(selectedAttrs);
    }

    private AttributeRef buildAttributeRef(SelectItem<?> item, List<String> availableAttrs) {
        var attrName = extractAttributeName(item);

        if (attrName == null) {
            throw new IllegalArgumentException(
                "Aggregate functions require an alias. Use: " + item.getExpression() + " AS alias_name"
            );
        }

        var resolvedAttrName = AttributeResolver.resolve(attrName, availableAttrs);
        var alias = item.getAlias() != null ? item.getAlias().getName() : attrName;

        return new AttributeRef(resolvedAttrName, alias);
    }

    private IRPipeline buildReturnForGroupBy(PlainSelect select, IRPipeline pipeline) {
        var selectItems = select.getSelectItems();
        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());

        if (selectItems.size() == 1 && selectItems.getFirst().getExpression() instanceof AllColumns) {
            return pipeline.returnAll();
        }

        var selectedAttrs = new ArrayList<AttributeRef>();

        for (var selectItem : selectItems) {
            var expr = selectItem.getExpression();

            if (expr instanceof AllColumns) continue;

            String alias;
            String resolvedAttrName;

            if (expr instanceof Function fn && isAggregate(fn)) {
                alias = selectItem.getAlias() != null
                    ? selectItem.getAlias().getName()
                    : generateDefaultAggregateAlias(fn);
                resolvedAttrName = alias;
            } else {
                var attrName = extractAttributeName(selectItem);
                resolvedAttrName = AttributeResolver.resolve(attrName, availableAttrs);
                alias = selectItem.getAlias() != null
                    ? selectItem.getAlias().getName()
                    : attrName;
            }

            selectedAttrs.add(new AttributeRef(resolvedAttrName, alias));
        }

        return pipeline.returnSelected(selectedAttrs);
    }

    private String generateDefaultAggregateAlias(Function fn) {
        var functionName = fn.getName().toLowerCase();
        var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
            ? toExpression(fn.getParameters().getFirst())
            : new IRExpression.ColumnRef("*");
        return functionName + "_" + argument.toString();
    }

    private IRPipeline addGroupBy(PlainSelect select, IRPipeline pipeline) {
        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());

        List<String> groupingAttributes;
        if (select.getGroupBy() != null) {
            var groupByExprs = new ArrayList<String>();
            for (var expr : select.getGroupBy().getGroupByExpressionList()) {
                if (expr instanceof Column col) {
                    var colRef = toColumnRef(col);
                    groupByExprs.add(AttributeResolver.resolve(colRef.columnName(), availableAttrs));
                }
            }
            groupingAttributes = groupByExprs;
        } else {
            groupingAttributes = List.of();
        }

        var selectItems = select.getSelectItems();
        var aggregates = new ArrayList<IRExpression.Aggregate>();

        for (var selectItem : selectItems) {
            var expr = selectItem.getExpression();
            if (expr instanceof AllColumns) continue;

            if (expr instanceof Function fn && isAggregate(fn)) {
                var functionName = fn.getName().toUpperCase();
                var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
                    ? toExpression(fn.getParameters().getFirst())
                    : new IRExpression.ColumnRef("*");

                String alias;
                if (selectItem.getAlias() != null) {
                    alias = selectItem.getAlias().getName();
                } else {
                    alias = functionName.toLowerCase() + "_" + argument.toString();
                }

                var qualifiedArgument = qualifyAggregateArgument(argument, availableAttrs);
                aggregates.add(new IRExpression.Aggregate(functionName, qualifiedArgument, alias));
            }
        }

        var outputAttributes = new ArrayList<String>();
        outputAttributes.addAll(groupingAttributes);
        outputAttributes.addAll(aggregates.stream()
            .map(IRExpression.Aggregate::alias)
            .toList());

        return pipeline.group(groupingAttributes, aggregates, outputAttributes);
    }

    private IRExpression qualifyAggregateArgument(
        IRExpression expr, List<String> availableAttrs
    ) {
        return switch (expr) {
            case IRExpression.ColumnRef(var columnName) ->
                new IRExpression.ColumnRef(AttributeResolver.resolve(columnName, availableAttrs));
            case IRExpression.Literal lit -> lit;
            case IRExpression.Arithmetic _,
                 IRExpression.Aggregate _,
                 IRExpression.ScalarSubquery _ -> expr;
        };
    }
}
