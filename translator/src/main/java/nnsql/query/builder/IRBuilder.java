package nnsql.query.builder;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.*;
import nnsql.query.ir.Return.AttributeRef;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class IRBuilder {
    private final SchemaRegistry schema;
    private final AtomicInteger nodeIdCounter = new AtomicInteger(0);
    private final Map<String, Relation.Subquery> cteDefinitions = new LinkedHashMap<>();

    private static final Set<String> AGGREGATE_FUNCTIONS =
        Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

    public IRBuilder(SchemaRegistry schema) {
        this.schema = schema;
    }

    public IRNode build(PlainSelect select) {
        cteDefinitions.clear();

        if (select.getWithItemsList() != null) {
            for (var withItem : select.getWithItemsList()) {
                var cteName = withItem.getAliasName();
                var cteBody = (PlainSelect) withItem.getSelect().getSelect();
                var cteIR = buildSelect(cteBody, false);
                var attributes = AttributeResolver.collectFrom(cteIR);
                cteDefinitions.put(cteName, new Relation.Subquery(cteName, cteIR, attributes));
            }
        }

        return buildSelect(select, true);
    }

    private IRNode buildSelect(PlainSelect select, boolean topLevel) {
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

        if (topLevel) {
            var sortKeys = parseSortKeys(select, pipeline);
            var limit = parseLimit(select);
            if (!sortKeys.isEmpty() || limit != null) {
                pipeline = pipeline.sort(sortKeys, limit);
            }
        } else if ((select.getOrderByElements() != null && !select.getOrderByElements().isEmpty())
            || select.getLimit() != null) {
            throw new UnsupportedOperationException(
                "ORDER BY/LIMIT in subqueries or CTEs is not supported");
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
                var alias = Option.ofNullable(t.getAlias())
                    .map(Alias::getName)
                    .orElse(tableName);

                var cteDef = cteDefinitions.get(tableName);
                if (cteDef != null) {
                    yield new Relation.Subquery(alias, cteDef.ir(), cteDef.attributes());
                }

                var attributes = schema.getAttributes(tableName);

                if (attributes.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Table '%s' not found in schema. Please register the table schema first."
                            .formatted(tableName));
                }

                yield new Relation.Table(tableName, alias, attributes);
            }
            case ParenthesedSelect ps -> {
                var alias = Option.ofNullable(ps.getAlias())
                    .map(Alias::getName)
                    .orElseGet(() -> "subq" + nodeIdCounter.getAndIncrement());
                var subqueryIR = buildSelect((PlainSelect) ps.getSelect(), false);
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
            case Function fn -> toFunctionCall(fn);
            case CaseExpression caseExpr -> {
                var switchExpr = caseExpr.getSwitchExpression();
                var whens = caseExpr.getWhenClauses().stream()
                    .map(whenClause -> {
                        Condition condition;
                        if (switchExpr != null) {
                            condition = Condition.eq(
                                toExpression(switchExpr),
                                toExpression(whenClause.getWhenExpression())
                            );
                        } else {
                            condition = toCondition(whenClause.getWhenExpression());
                        }
                        return new IRExpression.WhenClause(
                            condition,
                            toExpression(whenClause.getThenExpression())
                        );
                    })
                    .toList();
                var elseExpr = Option.ofNullable(caseExpr.getElseExpression())
                    .map(this::toExpression);
                yield new IRExpression.CaseWhen(whens, elseExpr);
            }
            case ParenthesedExpressionList<?> p -> toExpression(p.getFirst());
            case ParenthesedSelect ps -> toScalarSubquery(ps);
            case Addition add -> new IRExpression.BinaryOp(
                toExpression(add.getLeftExpression()), "+", toExpression(add.getRightExpression()));
            case Subtraction sub -> new IRExpression.BinaryOp(
                toExpression(sub.getLeftExpression()), "-", toExpression(sub.getRightExpression()));
            case Multiplication mul -> new IRExpression.BinaryOp(
                toExpression(mul.getLeftExpression()), "*", toExpression(mul.getRightExpression()));
            case Division div -> new IRExpression.BinaryOp(
                toExpression(div.getLeftExpression()), "/", toExpression(div.getRightExpression()));
            case CastExpression cast ->
                new IRExpression.Cast(
                    toExpression(cast.getLeftExpression()),
                    cast.getColDataType().toString()
                );
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
                && !(fn.getParameters().getFirst() instanceof AllColumns)
            ? toExpression(fn.getParameters().getFirst())
            : IRExpression.number(1);
        return new IRExpression.Aggregate(functionName, argument, alias);
    }

    private IRExpression.FunctionCall toFunctionCall(Function fn) {
        if (fn.isDistinct()) {
            throw new UnsupportedOperationException("DISTINCT in scalar functions is not supported");
        }

        var name = Option.ofNullable(fn.getName())
            .map(String::strip)
            .flatMap(value -> value.isEmpty() ? Option.none() : Option.some(value))
            .orElseThrow(() -> new UnsupportedOperationException("Unnamed function is not supported"));

        var arguments = Option.ofNullable(fn.getParameters())
            .map(parameters -> parameters.stream()
                .map(Expression.class::cast)
                .map(param -> switch (param) {
                    case AllColumns _ -> throw new UnsupportedOperationException(
                        "Scalar functions with * arguments are not supported"
                    );
                    default -> toExpression(param);
                })
                .toList())
            .orElse(List.of());

        return new IRExpression.FunctionCall(name, arguments);
    }

    private IRExpression.ScalarSubquery toScalarSubquery(ParenthesedSelect ps) {
        var subqueryIR = buildSelect((PlainSelect) ps.getSelect(), false);
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
            case LikeExpression like -> toLikeCondition(like);
            case Between between -> {
                var left = toExpression(between.getLeftExpression());
                var start = toExpression(between.getBetweenExpressionStart());
                var end = toExpression(between.getBetweenExpressionEnd());
                yield between.isNot()
                    ? Condition.or(Condition.lt(left, start), Condition.gt(left, end))
                    : Condition.and(Condition.gte(left, start), Condition.lte(left, end));
            }
            case InExpression in -> toInCondition(in);
            case ParenthesedExpressionList<?> p -> toCondition(p.getFirst());
            default -> throw new UnsupportedOperationException(
                "Unsupported condition: " + expr.getClass().getSimpleName());
        };
    }

    private Condition toInCondition(InExpression in) {
        var normalized = normalizeInRightExpression(in.getRightExpression());
        var inPayloadCondition = switch (normalized.payload()) {
            case ExpressionList<?> list -> toInListCondition(in, list);
            case ParenthesedSelect _ ->
                throw new UnsupportedOperationException("IN with subquery is not supported");
            default -> throw new UnsupportedOperationException(
                "IN supports value lists and subqueries only"
            );
        };

        if (normalized.trailingPredicates().isEmpty()) {
            return inPayloadCondition;
        }

        var operands = new ArrayList<Condition>();
        operands.add(inPayloadCondition);
        normalized.trailingPredicates().forEach(expr -> operands.add(toCondition(expr)));
        return Condition.and(operands);
    }

    private Condition toInListCondition(InExpression in, ExpressionList<?> list) {
        if (list.isEmpty()) {
            throw new UnsupportedOperationException("IN with empty list is not supported");
        }

        var left = toExpression(in.getLeftExpression());
        var values = list.stream()
            .map(Expression.class::cast)
            .map(this::toExpression)
            .map(value -> switch (value) {
                case IRExpression.Literal lit -> lit;
                default -> throw new UnsupportedOperationException(
                    "IN list supports literal values only"
                );
            })
            .toList();

        return in.isNot()
            ? Condition.and(values.stream().map(v -> (Condition) Condition.neq(left, v)).toList())
            : Condition.or(values.stream().map(v -> (Condition) Condition.eq(left, v)).toList());
    }

    private InRightExpression normalizeInRightExpression(Expression rightExpression) {
        if (!(rightExpression instanceof AndExpression and)) {
            return new InRightExpression(rightExpression, List.of());
        }

        var operands = new ArrayList<Expression>();
        flattenAndExpressions(and, operands);
        var payload = operands.getFirst();
        var trailingPredicates = new ArrayList<Expression>();
        if (operands.size() > 1) {
            trailingPredicates.addAll(operands.subList(1, operands.size()));
        }
        return new InRightExpression(payload, trailingPredicates);
    }

    private void flattenAndExpressions(Expression expr, List<Expression> operands) {
        if (expr instanceof AndExpression and) {
            flattenAndExpressions(and.getLeftExpression(), operands);
            flattenAndExpressions(and.getRightExpression(), operands);
        } else {
            operands.add(expr);
        }
    }

    private record InRightExpression(Expression payload, List<Expression> trailingPredicates) {
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

    private Condition toLikeCondition(LikeExpression like) {
        if (like.getEscape() != null) {
            throw new UnsupportedOperationException("LIKE ... ESCAPE is not supported");
        }
        if (like.isCaseInsensitive()) {
            throw new UnsupportedOperationException("Case-insensitive LIKE is not supported");
        }

        var left = toExpression(like.getLeftExpression());
        var pattern = toExpression(like.getRightExpression());
        return like.isNot()
            ? Condition.notLike(left, pattern)
            : Condition.like(left, pattern);
    }

    private boolean hasAggregatesInSelect(PlainSelect select) {
        for (var item : select.getSelectItems()) {
            if (item.getExpression() instanceof Function fn && isAggregate(fn)) {
                return true;
            }
        }
        return false;
    }

    private IRPipeline buildReturnForNonGroupBy(PlainSelect select, IRPipeline pipeline) {
        var selectItems = select.getSelectItems();

        if (isSelectAll(selectItems)) {
            return pipeline.returnAll();
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectedAttrs = IntStream.range(0, selectItems.size())
            .mapToObj(i -> buildNonGroupByAttributeRef(selectItems.get(i), availableAttrs, i + 1))
            .flatMap(Option::stream)
            .toList();

        return pipeline.returnSelected(selectedAttrs);
    }

    private Option<AttributeRef> buildNonGroupByAttributeRef(
        SelectItem<?> item,
        List<String> availableAttrs,
        int position
    ) {
        if (item.getExpression() instanceof AllColumns) {
            return Option.none();
        }
        return Option.some(buildAttributeRef(item, availableAttrs, position));
    }

    private AttributeRef buildAttributeRef(SelectItem<?> item, List<String> availableAttrs, int position) {
        var irExpr = toExpression(item.getExpression());

        return switch (irExpr) {
            case IRExpression.ColumnRef(var colName) -> {
                var resolvedName = AttributeResolver.resolve(colName, availableAttrs);
                var alias = selectItemAlias(item, OutputAlias.column(colName));
                yield AttributeRef.attr(resolvedName, alias);
            }
            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ -> {
                var alias = selectItemAlias(item, OutputAlias.expression(position));
                var qualifiedExpr = AttributeResolver.qualifyExpression(irExpr, availableAttrs);
                yield AttributeRef.expr(qualifiedExpr, alias);
            }
            case IRExpression.Aggregate agg -> {
                var alias = selectItemAlias(item, OutputAlias.aggregate(agg));
                yield AttributeRef.attr(alias, alias);
            }
            case IRExpression.Literal _, IRExpression.ScalarSubquery _ -> {
                var alias = selectItemAlias(item, OutputAlias.expression(position));
                var qualifiedExpr = AttributeResolver.qualifyExpression(irExpr, availableAttrs);
                yield AttributeRef.expr(qualifiedExpr, alias);
            }
        };
    }

    private String selectItemAlias(SelectItem<?> item, OutputAlias fallbackAlias) {
        return Option.ofNullable(item.getAlias())
            .map(Alias::getName)
            .orElseGet(fallbackAlias::toString);
    }

    private IRPipeline buildReturnForGroupBy(PlainSelect select, IRPipeline pipeline) {
        var selectItems = select.getSelectItems();
        if (isSelectAll(selectItems)) {
            return pipeline.returnAll();
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectedAttrs = IntStream.range(0, selectItems.size())
            .mapToObj(i -> buildGroupByAttributeRef(selectItems.get(i), availableAttrs, i + 1))
            .flatMap(Option::stream)
            .toList();

        return pipeline.returnSelected(selectedAttrs);
    }

    private Option<AttributeRef> buildGroupByAttributeRef(
        SelectItem<?> selectItem,
        List<String> availableAttrs,
        int position
    ) {
        var expr = selectItem.getExpression();
        if (expr instanceof AllColumns) {
            return Option.none();
        }

        if (expr instanceof Function fn && isAggregate(fn)) {
            var alias = selectItemAlias(selectItem, OutputAlias.aggregate(toAggregate(fn, null)));
            return Option.some(AttributeRef.attr(alias, alias));
        }

        var irExpr = toExpression(expr);
        return switch (irExpr) {
            case IRExpression.ColumnRef(var colName) -> {
                var resolvedName = AttributeResolver.resolve(colName, availableAttrs);
                var alias = selectItemAlias(selectItem, OutputAlias.column(colName));
                yield Option.some(AttributeRef.attr(resolvedName, alias));
            }
            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ -> {
                var alias = selectItemAlias(selectItem, OutputAlias.expression(position));
                var qualifiedExpr = AttributeResolver.qualifyExpression(irExpr, availableAttrs);
                yield Option.some(AttributeRef.expr(qualifiedExpr, alias));
            }
            default -> throw new UnsupportedOperationException(
                "Unsupported expression in GROUP BY SELECT: " + irExpr.getClass().getSimpleName()
            );
        };
    }

    private IRPipeline addGroupBy(PlainSelect select, IRPipeline pipeline) {
        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectAliasSourceAttributes = new LinkedHashMap<String, String>();
        var selectAliasUnsupportedExpressionTypes = new LinkedHashMap<String, String>();
        buildGroupByAliasBindings(
            select.getSelectItems(),
            availableAttrs,
            selectAliasSourceAttributes,
            selectAliasUnsupportedExpressionTypes
        );

        List<String> groupingAttributes;
        if (select.getGroupBy() != null) {
            var groupByExprs = new ArrayList<String>();
            for (var expr : select.getGroupBy().getGroupByExpressionList()) {
                if (expr instanceof Column col) {
                    groupByExprs.add(
                        resolveGroupByAttribute(
                            col,
                            availableAttrs,
                            selectAliasSourceAttributes,
                            selectAliasUnsupportedExpressionTypes
                        )
                    );
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
                var baseAggregate = toAggregate(fn, null);
                var argument = baseAggregate.argument();
                var alias = selectItemAlias(
                    selectItem,
                    OutputAlias.aggregate(baseAggregate)
                );

                var qualifiedArgument = qualifyAggregateArgument(argument, availableAttrs);
                aggregates.add(new IRExpression.Aggregate(baseAggregate.function(), qualifiedArgument, alias));
            }
        }

        var outputAttributes = new ArrayList<String>();
        outputAttributes.addAll(groupingAttributes);
        outputAttributes.addAll(aggregates.stream()
            .map(IRExpression.Aggregate::alias)
            .toList());

        return pipeline.group(groupingAttributes, aggregates, outputAttributes);
    }

    private void buildGroupByAliasBindings(
        List<SelectItem<?>> selectItems,
        List<String> availableAttrs,
        Map<String, String> aliasToSourceAttributes,
        Map<String, String> aliasToUnsupportedExpressionTypes
    ) {
        for (var selectItem : selectItems) {
            var expression = selectItem.getExpression();
            if (expression instanceof AllColumns) {
                continue;
            }

            Option.ofNullable(selectItem.getAlias())
                .map(Alias::getName)
                .stream()
                .forEach(aliasName -> bindGroupByAlias(
                    aliasName,
                    expression,
                    availableAttrs,
                    aliasToSourceAttributes,
                    aliasToUnsupportedExpressionTypes
                ));
        }
    }

    private void bindGroupByAlias(
        String aliasName,
        Expression expression,
        List<String> availableAttrs,
        Map<String, String> aliasToSourceAttributes,
        Map<String, String> aliasToUnsupportedExpressionTypes
    ) {
        var irExpression = toExpression(expression);
        switch (irExpression) {
            case IRExpression.ColumnRef(var columnName) ->
                aliasToSourceAttributes.put(
                    aliasName,
                    AttributeResolver.resolve(columnName, availableAttrs)
                );
            default ->
                aliasToUnsupportedExpressionTypes.put(
                    aliasName,
                    irExpression.getClass().getSimpleName()
                );
        }
    }

    private String resolveGroupByAttribute(
        Column column,
        List<String> availableAttrs,
        Map<String, String> selectAliasSourceAttributes,
        Map<String, String> selectAliasUnsupportedExpressionTypes
    ) {
        var columnRef = toColumnRef(column).columnName();

        try {
            return AttributeResolver.resolve(columnRef, availableAttrs);
        } catch (IllegalArgumentException error) {
            return switch (Option.ofNullable(selectAliasSourceAttributes.get(columnRef))) {
                case Option.Some(var sourceAttribute) -> sourceAttribute;
                case Option.None() -> {
                    var expressionType = Option.ofNullable(
                        selectAliasUnsupportedExpressionTypes.get(columnRef)
                    ).orElseThrow(() -> error);
                    throw new UnsupportedOperationException(
                        (
                            "GROUP BY alias '%s' resolves to unsupported %s expression; " +
                                "only simple column aliases are supported"
                        ).formatted(columnRef, expressionType)
                    );
                }
            };
        }
    }

    private boolean isSelectAll(List<SelectItem<?>> selectItems) {
        return selectItems.size() == 1 && selectItems.getFirst().getExpression() instanceof AllColumns;
    }

    private List<Sort.SortKey> parseSortKeys(PlainSelect select, IRPipeline pipeline) {
        var orderByElements = select.getOrderByElements();
        if (orderByElements == null || orderByElements.isEmpty()) {
            return List.of();
        }

        if (isSelectStarOutput(pipeline.build())) {
            throw new UnsupportedOperationException("ORDER BY is not supported with SELECT *");
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        return orderByElements.stream()
            .map(orderBy -> toSortKey(orderBy, availableAttrs))
            .toList();
    }

    private Sort.SortKey toSortKey(OrderByElement orderBy, List<String> availableAttrs) {
        if (orderBy.getNullOrdering() != null) {
            throw new UnsupportedOperationException("ORDER BY NULLS FIRST/LAST is not supported");
        }

        var sortAttribute = switch (orderBy.getExpression()) {
            case Column col -> AttributeResolver.resolve(toColumnRef(col).columnName(), availableAttrs);
            case LongValue ordinal -> resolveSortOrdinal(ordinal, availableAttrs);
            default -> throw new UnsupportedOperationException(
                "ORDER BY supports column references, aliases, and ordinals only");
        };

        var descending = orderBy.isAscDescPresent() && !orderBy.isAsc();
        return new Sort.SortKey(sortAttribute, descending);
    }

    private String resolveSortOrdinal(LongValue ordinalExpr, List<String> availableAttrs) {
        var ordinal = Math.toIntExact(ordinalExpr.getValue());
        if (ordinal < 1 || ordinal > availableAttrs.size()) {
            throw new IllegalArgumentException(
                "ORDER BY position %d is out of range for %d select items"
                    .formatted(ordinal, availableAttrs.size()));
        }
        return availableAttrs.get(ordinal - 1);
    }

    private Integer parseLimit(PlainSelect select) {
        var limit = select.getLimit();
        if (limit == null) {
            return null;
        }

        if (limit.getOffset() != null) {
            throw new UnsupportedOperationException("LIMIT with OFFSET is not supported");
        }

        var rowCount = limit.getRowCount();
        if (rowCount == null) {
            throw new UnsupportedOperationException("LIMIT requires an integer row count");
        }

        return switch (rowCount) {
            case LongValue lv -> {
                var value = Math.toIntExact(lv.getValue());
                if (value < 0) {
                    throw new IllegalArgumentException("LIMIT must be non-negative");
                }
                yield value;
            }
            default -> throw new UnsupportedOperationException(
                "LIMIT supports integer literals only");
        };
    }

    private boolean isSelectStarOutput(IRNode node) {
        return switch (node) {
            case Return ret -> ret.selectStar();
            case DuplElim de -> isSelectStarOutput(de.input());
            case Sort sort -> isSelectStarOutput(sort.input());
            default -> false;
        };
    }

    private IRExpression qualifyAggregateArgument(
        IRExpression expr, List<String> availableAttrs
    ) {
        return switch (expr) {
            case IRExpression.ColumnRef(var columnName) ->
                new IRExpression.ColumnRef(AttributeResolver.resolve(columnName, availableAttrs));
            case IRExpression.Literal lit -> lit;
            case IRExpression.Aggregate _,
                 IRExpression.ScalarSubquery _ -> expr;
            case IRExpression.BinaryOp(var left, var op, var right) ->
                new IRExpression.BinaryOp(
                    qualifyAggregateArgument(left, availableAttrs),
                    op,
                    qualifyAggregateArgument(right, availableAttrs));
            case IRExpression.Cast(var inner, var targetType) ->
                new IRExpression.Cast(
                    qualifyAggregateArgument(inner, availableAttrs),
                    targetType
                );
            case IRExpression.FunctionCall(var name, var arguments) ->
                new IRExpression.FunctionCall(
                    name,
                    arguments.stream()
                        .map(arg -> qualifyAggregateArgument(arg, availableAttrs))
                        .toList()
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var qualifiedWhens = whens.stream()
                    .map(when -> new IRExpression.WhenClause(
                        AttributeResolver.qualifyCondition(when.condition(), availableAttrs),
                        qualifyAggregateArgument(when.result(), availableAttrs)
                    ))
                    .toList();
                var qualifiedElse = qualifyAggregateArgument(elseExpr, availableAttrs);
                yield new IRExpression.CaseWhen(qualifiedWhens, qualifiedElse);
            }
        };
    }

    private Option<IRExpression> qualifyAggregateArgument(
        Option<IRExpression> expr, List<String> availableAttrs
    ) {
        return expr.map(value -> qualifyAggregateArgument(value, availableAttrs));
    }
}
