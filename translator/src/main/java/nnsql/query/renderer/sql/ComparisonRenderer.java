package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.*;
import nnsql.query.renderer.RenderContext;

import java.util.List;
import java.util.function.BiFunction;

import static nnsql.query.renderer.sql.Sql.*;

record ComparisonRenderer(BiFunction<IRNode, RenderContext, String> subqueryRenderer) {

    Expression renderTrue(Condition.Comparison comp, String rel, RenderContext ctx) {
        return render(comp, rel, false, ctx);
    }

    Expression renderFalse(Condition.Comparison comp, String rel, RenderContext ctx) {
        return render(comp, rel, true, ctx);
    }

    private Expression render(Condition.Comparison comp, String rel, boolean negate, RenderContext ctx) {
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

    private Expression renderWithColumn(String col, IRExpression right, String op, String rel,
                                         boolean negate, RenderContext ctx) {
        return switch (right) {
            case IRExpression.ColumnRef(var rightCol) ->
                existsColumnToColumn(rel, col, rightCol, op, negate);

            case IRExpression.Literal lit ->
                existsColumnToLiteral(rel, col, op, lit, negate);

            case IRExpression.ScalarSubquery subq ->
                existsColumnToSubquery(rel, col, op, renderSubquery(subq, ctx), negate);

            case IRExpression.Aggregate _ ->
                throw unsupported(right.getClass().getSimpleName());
        };
    }

    private Expression renderWithLiteral(IRExpression.Literal lit, IRExpression right, String op,
                                          String rel, boolean negate, RenderContext ctx) {
        return switch (right) {
            case IRExpression.ColumnRef(var col) ->
                existsLiteralToColumn(rel, col, op, lit, negate);

            case IRExpression.Literal rightLit ->
                new BooleanValue(evaluateConstant(lit, rightLit, op) != negate);

            case IRExpression.ScalarSubquery subq -> {
                var subSelect = renderSubquery(subq, ctx);
                var sub = new ParenthesedSelect();
                sub.setSelect(subSelect);
                yield comparison(literal(lit), op, sub);
            }

            case IRExpression.Aggregate _ ->
                throw unsupported(right.getClass().getSimpleName());
        };
    }

    private Expression existsColumnToColumn(String rel, String leftCol, String rightCol,
                                             String op, boolean negate) {
        Table leftTable = table(attrTable(rel, leftCol));
        Table rightTable = table(attrTable(rel, rightCol));
        Table idTbl = table(idTable(rel));

        Expression comp = comparison(column(leftTable, "v"), op, column(rightTable, "v"));
        if (negate) comp = not(paren(comp));

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(leftTable);
        ps.addJoins(simpleJoin(rightTable));
        ps.setWhere(andAll(List.of(
            new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                column(leftTable, "id"), column(idTbl, "id")),
            new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                column(rightTable, "id"), column(idTbl, "id")),
            comp
        )));

        return exists(ps);
    }

    private Expression existsColumnToLiteral(String rel, String col, String op,
                                              IRExpression.Literal lit, boolean negate) {
        Table attrTbl = table(attrTable(rel, col));
        Table idTbl = table(idTable(rel));

        Expression comp = comparison(column(attrTbl, "v"), op, literal(lit));
        if (negate) comp = not(paren(comp));

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(attrTbl);
        ps.setWhere(and(
            new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                column(attrTbl, "id"), column(idTbl, "id")),
            comp
        ));

        return exists(ps);
    }

    private Expression existsColumnToSubquery(String rel, String col, String op,
                                               PlainSelect subquery, boolean negate) {
        Table attrTbl = table(attrTable(rel, col));
        Table idTbl = table(idTable(rel));

        var sub = new ParenthesedSelect();
        sub.setSelect(subquery);
        Expression comp = comparison(column(attrTbl, "v"), op, sub);
        if (negate) comp = not(paren(comp));

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(attrTbl);
        ps.setWhere(and(
            new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                column(attrTbl, "id"), column(idTbl, "id")),
            comp
        ));

        return exists(ps);
    }

    private Expression existsLiteralToColumn(String rel, String col, String op,
                                              IRExpression.Literal lit, boolean negate) {
        Table attrTbl = table(attrTable(rel, col));
        Table idTbl = table(idTable(rel));

        Expression comp = comparison(literal(lit), op, column(attrTbl, "v"));
        if (negate) comp = not(paren(comp));

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(attrTbl);
        ps.setWhere(and(
            new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                column(attrTbl, "id"), column(idTbl, "id")),
            comp
        ));

        return exists(ps);
    }

    private PlainSelect renderSubquery(IRExpression.ScalarSubquery subquery, RenderContext ctx) {
        if (subquery.subqueryPipeline().isEmpty()) {
            throw new IllegalStateException("Scalar subquery has empty pipeline");
        }

        var subqueryIR = subquery.subqueryPipeline().getFirst();
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
        var ps = new PlainSelect();
        ps.addSelectItem(column("v"));
        ps.setFromItem(table(attrCTE(finalBaseName, attr.alias())));
        return ps;
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
