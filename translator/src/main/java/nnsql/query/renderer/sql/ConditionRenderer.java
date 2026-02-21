package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.Condition;
import nnsql.query.ir.IRNode;
import nnsql.query.renderer.RenderContext;

import java.util.List;
import java.util.function.BiFunction;

import static nnsql.query.renderer.sql.Sql.*;

record ConditionRenderer(ComparisonRenderer comparisonRenderer) {

    ConditionRenderer(BiFunction<IRNode, RenderContext, String> subqueryRenderer) {
        this(new ComparisonRenderer(subqueryRenderer));
    }

    Expression renderTrue(Condition condition, String relationName, RenderContext ctx) {
        return render(condition, relationName, false, ctx);
    }

    Expression renderFalse(Condition condition, String relationName, RenderContext ctx) {
        return render(condition, relationName, true, ctx);
    }

    private Expression render(Condition condition, String relationName, boolean negate, RenderContext ctx) {
        return switch (condition) {
            case Condition.Comparison comp ->
                negate ? comparisonRenderer.renderFalse(comp, relationName, ctx)
                       : comparisonRenderer.renderTrue(comp, relationName, ctx);

            case Condition.IsNull(var attr, var negated) ->
                renderIsNull(attr, negated == negate, relationName);

            case Condition.Like like ->
                renderLike(like, negate, relationName, ctx);

            case Condition.Exists exists ->
                renderExists(exists, negate, ctx);

            case Condition.InSubquery inSubquery ->
                comparisonRenderer.renderInSubquery(inSubquery, relationName, negate, ctx);

            case Condition.And and ->
                renderLogical(and.operands(), negate, relationName, ctx, !negate);

            case Condition.Or or ->
                renderLogical(or.operands(), negate, relationName, ctx, negate);

            case Condition.Not not ->
                render(not.operand(), relationName, !negate, ctx);
        };
    }

    private Expression renderLike(Condition.Like like, boolean negate, String relationName, RenderContext ctx) {
        var effectiveNegate = negate != like.isNegated();
        var operator = effectiveNegate ? "NOT LIKE" : "LIKE";
        var comparison = Condition.compare(like.left(), operator, like.pattern());
        return comparisonRenderer.renderTrue(comparison, relationName, ctx);
    }

    private Expression renderExists(Condition.Exists existsCondition, boolean negate, RenderContext ctx) {
        var effectiveNegate = negate != existsCondition.isNegated();
        var subquery = comparisonRenderer.renderExistsSubquery(existsCondition.subquery(), ctx);
        return effectiveNegate ? notExists(subquery) : exists(subquery);
    }

    private Expression renderIsNull(String attr, boolean isNull, String relationName) {
        var attrTbl = table(attrTable(relationName, attr));
        var idTbl = table(idTable(relationName));

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(attrTbl);
        ps.setWhere(new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
            column(attrTbl, "id"), column(idTbl, "id")
        ));

        return isNull ? notExists(ps) : exists(ps);
    }

    private Expression renderLogical(List<Condition> operands, boolean negate,
                                      String relationName, RenderContext ctx, boolean useAnd) {
        var rendered = operands.stream()
            .map(cond -> paren(render(cond, relationName, negate, ctx)))
            .toList();

        return useAnd ? andAll(rendered) : orAll(rendered);
    }
}
