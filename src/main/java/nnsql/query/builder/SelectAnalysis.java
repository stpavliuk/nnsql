package nnsql.query.builder;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.IRExpression;
import nnsql.query.ir.Return.AttributeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

record SelectAnalysis(
    boolean star,
    List<SelectItem<?>> selectItems,
    boolean hasGroupBy,
    List<Expression> groupByExprs,
    boolean hasAggregates
) {

    static SelectAnalysis of(PlainSelect select) {
        var items = select.getSelectItems();
        boolean star = items.size() == 1 && items.getFirst().getExpression() instanceof AllColumns;
        boolean hasGroupBy = select.getGroupBy() != null;
        var groupByExprs = hasGroupBy
            ? select.getGroupBy().getGroupByExpressionList().stream().map(e -> (Expression) e).toList()
            : List.<Expression>of();
        boolean hasAggregates = items.stream()
            .anyMatch(item -> item.getExpression() instanceof Function fn && Expressions.isAggregate(fn));
        return new SelectAnalysis(star, items, hasGroupBy, groupByExprs, hasAggregates);
    }

    List<String> resolveGroupByColumns(List<String> availableAttrs) {
        return groupByExprs.stream()
            .filter(Column.class::isInstance)
            .map(expr -> {
                var colRef = Expressions.columnRef((Column) expr);
                return AttributeResolver.resolve(colRef.columnName(), availableAttrs);
            })
            .toList();
    }

    List<IRExpression.Aggregate> extractAggregates(
        List<String> availableAttrs, SchemaRegistry schema, AtomicInteger nodeIds
    ) {
        var aggregates = new ArrayList<IRExpression.Aggregate>();

        for (var selectItem : selectItems) {
            var expr = selectItem.getExpression();
            if (expr instanceof AllColumns) continue;

            if (expr instanceof Function fn && Expressions.isAggregate(fn)) {
                var functionName = fn.getName().toUpperCase();
                var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
                    ? Expressions.from(fn.getParameters().getFirst(), schema, nodeIds)
                    : new IRExpression.ColumnRef("*");

                var alias = selectItem.getAlias() != null
                    ? selectItem.getAlias().getName()
                    : functionName.toLowerCase() + "_" + argument.toString();

                var qualifiedArg = qualifyAggregateArgument(argument, availableAttrs);
                aggregates.add(new IRExpression.Aggregate(functionName, qualifiedArg, alias));
            }
        }

        return aggregates;
    }

    List<AttributeRef> buildSelectRefs(
        List<String> availableAttrs, SchemaRegistry schema, AtomicInteger nodeIds
    ) {
        var refs = new ArrayList<AttributeRef>();

        for (var selectItem : selectItems) {
            var expr = selectItem.getExpression();
            if (expr instanceof AllColumns) continue;

            String attrName;
            String alias;

            if (expr instanceof Function fn && Expressions.isAggregate(fn)) {
                alias = selectItem.getAlias() != null
                    ? selectItem.getAlias().getName()
                    : defaultAggregateAlias(fn, schema, nodeIds);
                attrName = alias;
            } else {
                attrName = extractAttributeName(selectItem, schema, nodeIds);
                alias = selectItem.getAlias() != null
                    ? selectItem.getAlias().getName()
                    : attrName;
            }

            var resolvedAttrName = AttributeResolver.resolve(attrName, availableAttrs);
            refs.add(new AttributeRef(resolvedAttrName, alias));
        }

        return refs;
    }

    private static String extractAttributeName(SelectItem<?> item, SchemaRegistry schema, AtomicInteger nodeIds) {
        return switch (Expressions.from(item.getExpression(), schema, nodeIds)) {
            case IRExpression.ColumnRef col -> col.columnName();
            case IRExpression.Literal _ ->
                throw new IllegalArgumentException("Cannot use literal in SELECT without alias");
            case IRExpression.Aggregate agg -> agg.alias();
            case IRExpression.ScalarSubquery _ ->
                throw new IllegalArgumentException("Cannot use scalar subquery in SELECT without alias");
        };
    }

    private static String defaultAggregateAlias(Function fn, SchemaRegistry schema, AtomicInteger nodeIds) {
        var functionName = fn.getName().toLowerCase();
        var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
            ? Expressions.from(fn.getParameters().getFirst(), schema, nodeIds)
            : new IRExpression.ColumnRef("*");
        return functionName + "_" + argument.toString();
    }

    private static IRExpression qualifyAggregateArgument(IRExpression expr, List<String> availableAttrs) {
        return switch (expr) {
            case IRExpression.ColumnRef(var columnName) ->
                new IRExpression.ColumnRef(AttributeResolver.resolve(columnName, availableAttrs));
            case IRExpression.Literal lit -> lit;
            case IRExpression.Aggregate _,
                 IRExpression.ScalarSubquery _ -> expr;
        };
    }
}
