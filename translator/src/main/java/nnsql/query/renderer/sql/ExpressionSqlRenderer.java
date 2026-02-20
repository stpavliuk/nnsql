package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.CastExpression;

import nnsql.query.ir.IRExpression;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static nnsql.query.renderer.sql.Sql.*;

final class ExpressionSqlRenderer {

    private ExpressionSqlRenderer() {
        throw new UnsupportedOperationException("Utility class");
    }

    static List<String> collectColumns(IRExpression expr) {
        return switch (expr) {
            case IRExpression.ColumnRef(var col) -> List.of(col);
            case IRExpression.Literal _ -> List.of();
            case IRExpression.BinaryOp(var left, _, var right) ->
                Stream.concat(collectColumns(left).stream(), collectColumns(right).stream())
                    .distinct().toList();
            case IRExpression.Cast(var inner, _) -> collectColumns(inner);
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported expression in column collection");
        };
    }

    static List<String> collectColumns(IRExpression... exprs) {
        return Arrays.stream(exprs)
            .flatMap(expr -> collectColumns(expr).stream())
            .distinct()
            .toList();
    }

    static Expression toSqlExpr(IRExpression expr, String baseName) {
        return switch (expr) {
            case IRExpression.ColumnRef(var col) -> column(attrTable(baseName, col), "v");
            case IRExpression.Literal lit -> literal(lit);
            case IRExpression.BinaryOp(var left, var op, var right) ->
                arithmetic(toSqlExpr(left, baseName), op, toSqlExpr(right, baseName));
            case IRExpression.Cast(var inner, var targetType) ->
                new CastExpression("CAST", toSqlExpr(inner, baseName), targetType);
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported expression in SQL rendering");
        };
    }
}
