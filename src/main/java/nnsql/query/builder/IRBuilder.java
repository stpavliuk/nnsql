package nnsql.query.builder;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.*;
import nnsql.query.ir.Return.AttributeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IRBuilder {
    private final SchemaRegistry schema;
    private final AtomicInteger nodeIdCounter;

    public IRBuilder(SchemaRegistry schema) {
        this(schema, new AtomicInteger(0));
    }

    IRBuilder(SchemaRegistry schema, AtomicInteger nodeIdCounter) {
        this.schema = schema;
        this.nodeIdCounter = nodeIdCounter;
    }

    public IRNode build(PlainSelect select) {
        var pipeline = IRPipeline.start();

        var relations = Relations.from(select, schema, nodeIdCounter);
        pipeline = pipeline.product(relations);

        if (select.getWhere() != null) {
            var attributes = AttributeResolver.collectFrom(pipeline.build());
            var condition = Conditions.from(select.getWhere(), schema, nodeIdCounter).get();
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
            var condition = Conditions.from(select.getHaving(), schema, nodeIdCounter).get();
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

    private boolean hasAggregatesInSelect(PlainSelect select) {
        for (var item : select.getSelectItems()) {
            if (item.getExpression() instanceof Function fn && Expressions.isAggregate(fn)) {
                return true;
            }
        }
        return false;
    }

    private String extractAttributeName(SelectItem<?> item) {
        var expr = item.getExpression();
        return switch (Expressions.from(expr, schema, nodeIdCounter)) {
            case IRExpression.ColumnRef col -> col.columnName();
            case IRExpression.Literal _ ->
                throw new IllegalArgumentException("Cannot use literal in SELECT without alias");
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

            if (expr instanceof Function fn && Expressions.isAggregate(fn)) {
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
            ? Expressions.from(fn.getParameters().getFirst(), schema, nodeIdCounter)
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
                    var colRef = Expressions.columnRef(col);
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

            if (expr instanceof Function fn && Expressions.isAggregate(fn)) {
                var functionName = fn.getName().toUpperCase();
                var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
                    ? Expressions.from(fn.getParameters().getFirst(), schema, nodeIdCounter)
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
            case IRExpression.Aggregate _,
                 IRExpression.ScalarSubquery _ -> expr;
        };
    }
}
