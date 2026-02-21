package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.*;
import nnsql.query.renderer.RenderContext;

import java.util.ArrayList;
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

            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _ ->
                renderWithComputedExpr(comp.left(), comp.right(), comp.operator(), rel, negate);

            case IRExpression.ScalarSubquery _ ->
                throw unsupported("Scalar subquery on left side of comparison");

            case IRExpression.Aggregate _ ->
                throw unsupported(comp.left().getClass().getSimpleName());
        };
    }

    private Expression renderWithComputedExpr(IRExpression leftExpr, IRExpression right, String op,
                                              String rel, boolean negate) {
        var allCols = ExpressionSqlRenderer.collectColumns(leftExpr, right);

        return existsExprToExpr(rel, leftExpr, right, op, allCols, negate);
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

            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _ ->
                renderWithComputedExpr(new IRExpression.ColumnRef(col), right, op, rel, negate);

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

            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _ -> {
                var rightCols = ExpressionSqlRenderer.collectColumns(right);
                yield existsExprToExpr(rel, lit, right, op, rightCols, negate);
            }

            case IRExpression.Aggregate _ ->
                throw unsupported(right.getClass().getSimpleName());
        };
    }

    private Expression existsExprToExpr(String rel, IRExpression left, IRExpression right,
                                         String op, List<String> columns, boolean negate) {
        Table idTbl = table(idTable(rel));

        boolean hasCaseWhen = ExpressionSqlRenderer.containsCaseWhen(left)
            || ExpressionSqlRenderer.containsCaseWhen(right);

        Expression comp = comparison(
            ExpressionSqlRenderer.toSqlExpr(left, rel),
            op,
            ExpressionSqlRenderer.toSqlExpr(right, rel)
        );
        if (negate) comp = not(paren(comp));

        if (columns.isEmpty()) {
            return comp;
        }

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());

        if (hasCaseWhen) {
            Table cwIdTbl = tableAlias(idTable(rel), "cw_id");
            ps.setFromItem(cwIdTbl);

            var joins = new ArrayList<Join>();
            addComputedExprAttributeJoins(
                rel,
                columns,
                column("cw_id", "id"),
                true,
                Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
                joins,
                new ArrayList<>()
            );
            ps.setJoins(joins);

            ps.setWhere(and(
                new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                    column("cw_id", "id"), column(idTbl, "id")),
                comp));
        } else {
            var firstCol = columns.getFirst();
            Table firstTable = table(attrTable(rel, firstCol));
            ps.setFromItem(firstTable);

            var joins = new ArrayList<Join>();
            var conditions = new ArrayList<Expression>();
            conditions.add(new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                column(firstTable, "id"), column(idTbl, "id")));
            addComputedExprAttributeJoins(
                rel,
                columns.subList(1, columns.size()),
                column(idTbl, "id"),
                false,
                Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
                joins,
                conditions
            );

            if (!joins.isEmpty()) {
                ps.setJoins(joins);
            }

            conditions.add(comp);
            ps.setWhere(andAll(conditions));
        }

        return exists(ps);
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

    private Expression existsWithSingleAttr(String rel, String col, Expression comp, boolean negate) {
        if (negate) comp = not(paren(comp));

        Table attrTbl = table(attrTable(rel, col));
        Table idTbl = table(idTable(rel));

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

    private Expression existsColumnToLiteral(String rel, String col, String op,
                                              IRExpression.Literal lit, boolean negate) {
        Expression comp = comparison(column(table(attrTable(rel, col)), "v"), op, literal(lit));
        return existsWithSingleAttr(rel, col, comp, negate);
    }

    private Expression existsColumnToSubquery(String rel, String col, String op,
                                               PlainSelect subquery, boolean negate) {
        var sub = new ParenthesedSelect();
        sub.setSelect(subquery);
        Expression comp = comparison(column(table(attrTable(rel, col)), "v"), op, sub);
        return existsWithSingleAttr(rel, col, comp, negate);
    }

    private Expression existsLiteralToColumn(String rel, String col, String op,
                                              IRExpression.Literal lit, boolean negate) {
        Expression comp = comparison(literal(lit), op, column(table(attrTable(rel, col)), "v"));
        return existsWithSingleAttr(rel, col, comp, negate);
    }

    private PlainSelect renderSubquery(IRExpression.ScalarSubquery subquery, RenderContext ctx) {
        if (subquery.subqueryPipeline().isEmpty()) {
            throw new IllegalStateException("Scalar subquery has empty pipeline");
        }

        var subqueryIR = subquery.subqueryPipeline().getFirst();
        var finalBaseName = subqueryRenderer.apply(subqueryIR, ctx);

        var returnNode = IRNodeTraversal.findReturnNode(subqueryIR);
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
