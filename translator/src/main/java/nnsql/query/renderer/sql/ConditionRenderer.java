package nnsql.query.renderer.sql;

import nnsql.query.ir.Condition;
import nnsql.query.ir.IRNode;
import nnsql.query.renderer.RenderContext;

import java.util.function.BiFunction;
import java.util.stream.Collectors;

record ConditionRenderer(ComparisonRenderer comparisonRenderer) {

    ConditionRenderer(BiFunction<IRNode, RenderContext, String> subqueryRenderer) {
        this(new ComparisonRenderer(subqueryRenderer));
    }

    String renderTrue(Condition condition, String relationName, RenderContext ctx) {
        return render(condition, relationName, false, ctx);
    }

    String renderFalse(Condition condition, String relationName, RenderContext ctx) {
        return render(condition, relationName, true, ctx);
    }

    private String render(Condition condition, String relationName, boolean negate, RenderContext ctx) {
        return switch (condition) {
            case Condition.Comparison comp ->
                negate ? comparisonRenderer.renderFalse(comp, relationName, ctx)
                       : comparisonRenderer.renderTrue(comp, relationName, ctx);

            case Condition.IsNull(var attr, var negated) ->
                renderIsNull(attr, negated == negate, relationName);

            case Condition.And and ->
                renderLogical(and.operands(), negate ? " OR " : " AND ", relationName, negate, ctx);

            case Condition.Or or ->
                renderLogical(or.operands(), negate ? " AND " : " OR ", relationName, negate, ctx);

            case Condition.Not not ->
                render(not.operand(), relationName, !negate, ctx);
        };
    }

    private String renderIsNull(String attr, boolean isNull, String relationName) {
        String existsClause = "EXISTS (SELECT * FROM %s_%s WHERE %s_%s.id = %s_id.id)"
            .formatted(relationName, attr, relationName, attr, relationName);

        return isNull ? "NOT " + existsClause : existsClause;
    }

    private String renderLogical(java.util.List<Condition> operands, String separator,
                                 String relationName, boolean negate, RenderContext ctx) {
        return operands.stream()
            .map(cond -> "(" + render(cond, relationName, negate, ctx) + ")")
            .collect(Collectors.joining(separator));
    }
}
