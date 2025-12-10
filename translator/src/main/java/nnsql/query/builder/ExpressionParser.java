package nnsql.query.builder;

import nnsql.query.ir.Expression;
import nnsql.query.ir.IRNode;
import parser.sql.sqlParser;

import java.util.function.Function;

public class ExpressionParser {
    private final Function<sqlParser.SelectStmtContext, IRNode> subqueryBuilder;

    public ExpressionParser() {
        this(null);
    }

    public ExpressionParser(Function<sqlParser.SelectStmtContext, IRNode> subqueryBuilder) {
        this.subqueryBuilder = subqueryBuilder;
    }

    public Expression parse(sqlParser.ExprContext ctx) {
        return switch (ctx) {
            case sqlParser.ColumnExprContext colCtx -> parseColumnRef(colCtx);
            case sqlParser.LiteralExprContext litCtx -> parseLiteral(litCtx);
            case sqlParser.ParenExprContext parenCtx -> parse(parenCtx.expr());
            case sqlParser.MulExprContext mulCtx -> parseMultiplication(mulCtx);
            case sqlParser.AddSubExprContext addSubCtx -> parseAddSubtract(addSubCtx);
            case sqlParser.AggCallExprContext aggCtx -> parseAggregate(aggCtx);
            case sqlParser.ScalarSubqueryExprContext subCtx -> parseScalarSubquery(subCtx);
            default ->
                throw new IllegalArgumentException("Cannot parse expression: " + ctx.getText());
        };
    }

    public String extractAttributeName(sqlParser.ExprContext ctx) {
        return switch (parse(ctx)) {
            case Expression.ColumnRef col -> col.columnName();
            case Expression.Literal _ ->
                throw new IllegalArgumentException("Cannot use literal in SELECT without alias");
            case Expression.Arithmetic _ ->
                throw new IllegalArgumentException("Cannot use arithmetic expression in SELECT without alias");
            case Expression.Aggregate agg -> agg.alias();
            case Expression.ScalarSubquery _ ->
                throw new IllegalArgumentException("Cannot use scalar subquery in SELECT without alias");
        };
    }

    private Expression parseColumnRef(sqlParser.ColumnExprContext ctx) {
        var colRef = ctx.columnRef();
        String columnName;
        if (colRef.tableName() != null) {
            columnName = colRef.tableName().getText() + "_" + colRef.identifier().getText();
        } else {
            columnName = colRef.identifier().getText();
        }
        return new Expression.ColumnRef(columnName);
    }

    private Expression parseLiteral(sqlParser.LiteralExprContext ctx) {
        var literal = ctx.literal();
        if (literal.NULL_T() != null) {
            return new Expression.Literal(null, Expression.LiteralType.NULL);
        } else if (literal.NUMBER() != null) {
            String numText = literal.NUMBER().getText();
            double value = Double.parseDouble(numText);
            return new Expression.Literal(value, Expression.LiteralType.NUMBER);
        } else if (literal.STRING() != null) {
            String strText = literal.STRING().getText();
            String value = strText.substring(1, strText.length() - 1);
            return new Expression.Literal(value, Expression.LiteralType.STRING);
        } else {
            throw new IllegalArgumentException("Unknown literal type: " + literal.getText());
        }
    }

    private Expression parseMultiplication(sqlParser.MulExprContext ctx) {
        return new Expression.Arithmetic(
            parse(ctx.expr(0)),
            "*",
            parse(ctx.expr(1))
        );
    }

    private Expression parseAddSubtract(sqlParser.AddSubExprContext ctx) {
        String op = ctx.PLUS() != null ? "+" : "-";
        return new Expression.Arithmetic(
            parse(ctx.expr(0)),
            op,
            parse(ctx.expr(1))
        );
    }

    private Expression parseAggregate(sqlParser.AggCallExprContext ctx) {
        var functionName = ctx.aggFunc().getText().toUpperCase();
        var argument = parse(ctx.expr());

        return new Expression.Aggregate(functionName, argument, null);
    }

    private Expression parseScalarSubquery(sqlParser.ScalarSubqueryExprContext ctx) {
        if (subqueryBuilder == null) {
            throw new IllegalStateException(
                "Scalar subqueries not supported: ExpressionParser not configured with subquery builder"
            );
        }

        var selectStmt = ctx.selectStmt();
        var subqueryIR = subqueryBuilder.apply(selectStmt);

        var pipeline = new java.util.ArrayList<IRNode>();
        collectPipeline(subqueryIR, pipeline);

        return new Expression.ScalarSubquery(pipeline);
    }

    private void collectPipeline(IRNode node, java.util.List<IRNode> pipeline) {
        pipeline.addFirst(node);
    }
}
