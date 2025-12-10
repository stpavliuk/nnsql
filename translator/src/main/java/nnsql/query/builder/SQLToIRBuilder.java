package nnsql.query.builder;

import nnsql.query.SchemaRegistry;
import nnsql.query.ir.Expression;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Return.AttributeRef;
import parser.sql.sqlParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SQLToIRBuilder {
    private final ExpressionParser expressionParser;
    private final ConditionBuilder conditionBuilder;
    private final RelationBuilder  relationBuilder;

    public SQLToIRBuilder(SchemaRegistry schemaRegistry) {
        AtomicInteger nodeIdCounter = new AtomicInteger(0);
        this.expressionParser = new ExpressionParser(this::buildFromSelectStmt);
        this.conditionBuilder = new ConditionBuilder(expressionParser);

        this.relationBuilder = new RelationBuilder(schemaRegistry, this::buildFromSelectStmt, nodeIdCounter);
    }

    public IRNode buildFromSelectStmt(sqlParser.SelectStmtContext ctx) {
        var pipeline = IRPipeline.start();
        var relations = ctx
            .fromClause()
            .fromItem()
            .stream()
            .map(relationBuilder::build)
            .toList();

        pipeline = pipeline.product(relations);

        if (ctx.whereClause() != null) {
            var attributes = AttributeResolver.collectFrom(pipeline.build());
            var condition = conditionBuilder.build(ctx
                .whereClause()
                .boolExpr());
            var qualifiedCondition = AttributeResolver.qualifyCondition(condition, attributes);
            pipeline = pipeline.filter(qualifiedCondition, attributes);
        }

        boolean hasAggregates = hasAggregatesInSelect(ctx);
        boolean hasGroupBy = ctx.groupByClause() != null;

        if (hasGroupBy || hasAggregates) {
            pipeline = addGroupBy(ctx, pipeline);
        }

        if (ctx.havingClause() != null) {
            var attributes = AttributeResolver.collectFrom(pipeline.build());
            var condition = conditionBuilder.build(ctx.havingClause()
                                                      .boolExpr());
            var qualifiedCondition = AttributeResolver.qualifyCondition(condition, attributes);
            pipeline = pipeline.aggFilter(qualifiedCondition, attributes);
        }

        if (hasGroupBy || hasAggregates) {
            pipeline = buildReturnForGroupBy(ctx, pipeline);
        } else {
            pipeline = buildReturnForNonGroupBy(ctx, pipeline);
        }

        if (ctx.selectClause()
               .DISTINCT() != null) {
            var attributes = AttributeResolver.collectFrom(pipeline.build());
            pipeline = pipeline.duplElim(attributes);
        }

        return pipeline.build();
    }

    private AttributeRef buildAttributeRef(sqlParser.SelectItemContext item, List<String> availableAttrs) {
        if (item.STAR() != null) {
            throw new IllegalArgumentException("* cannot be mixed with other select items");
        }

        var attrName = expressionParser.extractAttributeName(item.expr());

        if (attrName == null) {
            throw new IllegalArgumentException(
                "Aggregate functions require an alias. Use: " + item.expr().getText() + " AS alias_name"
            );
        }

        var resolvedAttrName = AttributeResolver.resolve(attrName, availableAttrs);
        var alias = item.alias() != null ? item.alias()
                                               .getText() : attrName;

        return new AttributeRef(resolvedAttrName, alias);
    }

    private boolean isStar(sqlParser.SelectListContext selectList) {
        return selectList.selectItem()
                         .size() == 1 && selectList.selectItem(0)
                                                   .STAR() != null;
    }

    private IRPipeline buildReturnForNonGroupBy(sqlParser.SelectStmtContext ctx, IRPipeline pipeline) {
        var selectList = ctx.selectClause().selectList();

        if (isStar(selectList)) {
            return pipeline.returnAll();
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectedAttrs = selectList
            .selectItem()
            .stream()
            .filter(item -> item.STAR() == null)
            .map(item -> buildAttributeRef(item, availableAttrs))
            .toList();

        return pipeline.returnSelected(selectedAttrs);
    }

    private IRPipeline buildReturnForGroupBy(sqlParser.SelectStmtContext ctx, IRPipeline pipeline) {
        var selectList = ctx.selectClause().selectList();
        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());

        if (isStar(selectList)) {
            return pipeline.returnAll();
        }

        var selectedAttrs = new ArrayList<AttributeRef>();

        for (var selectItem : selectList.selectItem()) {
            if (selectItem.STAR() != null) {
                continue;
            }

            var expr = selectItem.expr();
            String alias;
            String resolvedAttrName;

            if (expr instanceof sqlParser.AggCallExprContext) {
                alias = selectItem.alias() != null
                    ? selectItem.alias().getText()
                    : generateDefaultAggregateAlias((sqlParser.AggCallExprContext) expr);

                resolvedAttrName = alias;
            } else {
                var attrName = expressionParser.extractAttributeName(expr);
                resolvedAttrName = AttributeResolver.resolve(attrName, availableAttrs);
                alias = selectItem.alias() != null
                    ? selectItem.alias().getText()
                    : attrName;
            }

            selectedAttrs.add(new AttributeRef(resolvedAttrName, alias));
        }

        return pipeline.returnSelected(selectedAttrs);
    }

    private boolean hasAggregatesInSelect(sqlParser.SelectStmtContext ctx) {
        var selectList = ctx.selectClause().selectList();

        for (var selectItem : selectList.selectItem()) {
            if (selectItem.expr() instanceof sqlParser.AggCallExprContext) {
                return true;
            }
        }
        return false;
    }

    private String generateDefaultAggregateAlias(sqlParser.AggCallExprContext aggCtx) {
        var functionName = aggCtx.aggFunc().getText().toLowerCase();
        var argument = expressionParser.parse(aggCtx.expr());
        return functionName + "_" + argument.toString();
    }

    private IRPipeline addGroupBy(sqlParser.SelectStmtContext ctx, IRPipeline pipeline) {
        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());

        List<String> groupingAttributes;
        if (ctx.groupByClause() != null) {
            var groupByList = ctx.groupByClause().groupByList();
            groupingAttributes = groupByList
                .expr().stream()
                .filter(expr -> expr instanceof sqlParser.ColumnExprContext)
                .map(expr -> {
                    var colCtx = ((sqlParser.ColumnExprContext) expr).columnRef();
                    String columnName;
                    if (colCtx.tableName() != null) {
                        columnName = colCtx.tableName().getText() + "_" + colCtx
                            .identifier()
                            .getText();
                    } else {
                        columnName = colCtx.identifier().getText();
                    }
                    return AttributeResolver.resolve(columnName, availableAttrs);
                })
                .toList();
        } else {
            groupingAttributes = List.of();
        }

        var selectList = ctx.selectClause().selectList();
        var aggregates = new java.util.ArrayList<Expression.Aggregate>();

        for (var selectItem : selectList.selectItem()) {
            if (selectItem.STAR() != null) continue;

            var expr = selectItem.expr();
            if (expr instanceof sqlParser.AggCallExprContext aggCtx) {
                var functionName = aggCtx.aggFunc().getText().toUpperCase();
                var argument = expressionParser.parse(aggCtx.expr());

                String alias;
                if (selectItem.alias() != null) {
                    alias = selectItem.alias().getText();
                } else {
                    alias = functionName.toLowerCase() + "_" + argument.toString();
                }

                var qualifiedArgument = qualifyAggregateArgument(argument, availableAttrs);
                aggregates.add(new Expression.Aggregate(functionName, qualifiedArgument, alias));
            }
        }

        var outputAttributes = new ArrayList<String>();
        outputAttributes.addAll(groupingAttributes);
        outputAttributes.addAll(aggregates
            .stream()
            .map(Expression.Aggregate::alias)
            .toList());

        return pipeline.group(groupingAttributes, aggregates, outputAttributes);
    }

    private Expression qualifyAggregateArgument(Expression expr, List<String> availableAttrs) {
        return switch (expr) {
            case Expression.ColumnRef(var columnName) ->
                new Expression.ColumnRef(AttributeResolver.resolve(columnName, availableAttrs));
            case Expression.Literal lit -> lit;
            case Expression.Arithmetic _, Expression.Aggregate _, Expression.ScalarSubquery _ -> expr;
        };
    }
}
