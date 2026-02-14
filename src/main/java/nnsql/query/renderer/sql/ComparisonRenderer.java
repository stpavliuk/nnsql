package nnsql.query.renderer.sql;

import nnsql.query.ir.*;
import nnsql.query.renderer.RenderContext;

import java.util.function.BiFunction;

record ComparisonRenderer(BiFunction<IRNode, RenderContext, String> subqueryRenderer) {

    String renderTrue(Condition.Comparison comp, String rel, RenderContext ctx) {
        return render(comp, rel, false, ctx);
    }

    String renderFalse(Condition.Comparison comp, String rel, RenderContext ctx) {
        return render(comp, rel, true, ctx);
    }

    private String render(Condition.Comparison comp, String rel, boolean negate, RenderContext ctx) {
        return switch (comp.left()) {
            case IRExpression.ColumnRef(var col) ->
                renderWithColumn(col, comp.right(), comp.operator(), rel, negate, ctx);

            case IRExpression.Literal lit ->
                renderWithLiteral(lit, comp.right(), comp.operator(), rel, negate, ctx);

            case IRExpression.ScalarSubquery _ ->
                throw unsupported("Scalar subquery on left side of comparison");

            case IRExpression.Aggregate _ ->
                throw unsupported(comp.left().getClass().getSimpleName());
        };
    }

    private String renderWithColumn(String col, IRExpression right, String op, String rel, boolean negate, RenderContext ctx) {
        return switch (right) {
            case IRExpression.ColumnRef(var rightCol) ->
                Format.existsColumnToColumn(rel, col, rightCol, op, negate);

            case IRExpression.Literal lit ->
                Format.existsColumnToLiteral(rel, col, op, Format.literal(lit), negate);

            case IRExpression.ScalarSubquery subq ->
                Format.existsColumnToSubquery(rel, col, op, renderSubquery(subq, ctx), negate);

            case IRExpression.Aggregate _ ->
                throw unsupported(right.getClass().getSimpleName());
        };
    }

    private String renderWithLiteral(IRExpression.Literal lit, IRExpression right, String op, String rel, boolean negate, RenderContext ctx) {
        return switch (right) {
            case IRExpression.ColumnRef(var col) ->
                Format.existsLiteralToColumn(rel, col, op, Format.literal(lit), negate);

            case IRExpression.Literal rightLit ->
                (evaluateConstant(lit, rightLit, op) != negate) ? "TRUE" : "FALSE";

            case IRExpression.ScalarSubquery subq ->
                "%s %s (%s)".formatted(Format.literal(lit), op, renderSubquery(subq, ctx));

            case IRExpression.Aggregate _ ->
                throw unsupported(right.getClass().getSimpleName());
        };
    }

    private String renderSubquery(IRExpression.ScalarSubquery subquery, RenderContext ctx) {
        var subqueryIR = subquery.subquery();
        var finalBaseName = subqueryRenderer.apply(subqueryIR, ctx);

        var returnNode = findReturnNode(subqueryIR);
        if (returnNode == null || returnNode.selectStar()) {
            throw new IllegalStateException("Scalar subquery must have a single selected attribute");
        }

        var attrs = returnNode.selectedAttributes();
        if (attrs.size() != 1) {
            throw new IllegalStateException("Scalar subquery must return exactly one column, got: " + attrs.size());
        }

        var attr = attrs.getFirst();
        return "SELECT v FROM " + Format.attrCTE(finalBaseName, attr.alias());
    }

    private Return findReturnNode(IRNode node) {
        return switch (node) {
            case Return r -> r;
            case DuplElim d -> findReturnNode(d.input());
            case AggFilter af -> findReturnNode(af.input());
            case Group g -> findReturnNode(g.input());
            case Filter f -> findReturnNode(f.input());
            case Product p -> null;
        };
    }

    private boolean evaluateConstant(IRExpression.Literal left, IRExpression.Literal right, String op) {
        if (left.value() instanceof Number l && right.value() instanceof Number r) {
            double lv = l.doubleValue();
            double rv = r.doubleValue();
            return switch (op) {
                case "=" -> lv == rv;
                case "!=" -> lv != rv;
                case "<" -> lv < rv;
                case ">" -> lv > rv;
                case "<=" -> lv <= rv;
                case ">=" -> lv >= rv;
                default -> throw new IllegalArgumentException("Unknown operator: " + op);
            };
        }

        throw new UnsupportedOperationException("Non-numeric constant comparison not supported");
    }

    private UnsupportedOperationException unsupported(String type) {
        return new UnsupportedOperationException(type + " expressions not yet supported");
    }
}
