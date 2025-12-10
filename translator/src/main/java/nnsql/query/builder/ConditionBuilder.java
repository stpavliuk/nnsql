package nnsql.query.builder;

import nnsql.query.ir.Condition;
import nnsql.query.ir.Expression;
import parser.sql.sqlParser;

public record ConditionBuilder(ExpressionParser expressionParser) {

    public Condition build(sqlParser.BoolExprContext ctx) {
        return buildOr(ctx.orExpr());
    }

    private Condition buildOr(sqlParser.OrExprContext ctx) {
        var andExprs = ctx.andExpr();
        if (andExprs.size() == 1) {
            return buildAnd(andExprs.getFirst());
        }

        var operands = andExprs
            .stream()
            .map(this::buildAnd)
            .toList();

        return new Condition.Or(operands);
    }

    private Condition buildAnd(sqlParser.AndExprContext ctx) {
        var notExprs = ctx.notExpr();
        if (notExprs.size() == 1) {
            return buildNot(notExprs.getFirst());
        }

        var operands = notExprs
            .stream()
            .map(this::buildNot)
            .toList();

        return new Condition.And(operands);
    }

    private Condition buildNot(sqlParser.NotExprContext ctx) {
        if (ctx.NOT() != null) {
            return new Condition.Not(buildNot(ctx.notExpr()));
        }

        return buildPredicate(ctx.predicate());
    }

    private Condition buildPredicate(sqlParser.PredicateContext ctx) {
        if (ctx.IS() != null) {
            return buildIsNull(ctx);
        }

        if (ctx.compOp() != null) {
            return buildComparison(ctx);
        }

        return build(ctx.boolExpr());
    }

    private Condition buildIsNull(sqlParser.PredicateContext ctx) {
        return switch (expressionParser.parse(ctx.expr(0))) {
            case Expression.ColumnRef(var columnName) -> new Condition.IsNull(columnName, ctx.NOT() != null);
            default -> throw new IllegalArgumentException("IS NULL can only be applied to columns, not literals");
        };
    }

    private Condition buildComparison(sqlParser.PredicateContext ctx) {
        var operator = ctx.compOp().getText();
        var leftExpr = expressionParser.parse(ctx.expr(0));
        var rightExpr = expressionParser.parse(ctx.expr(1));
        return Condition.compare(leftExpr, operator, rightExpr);
    }
}
