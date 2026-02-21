package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;

import nnsql.query.ir.Condition;
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
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var whenColumns = whens.stream()
                    .flatMap(when -> Stream.concat(
                        collectColumnsFromCondition(when.condition()).stream(),
                        collectColumns(when.result()).stream()
                    ));
                var elseColumns = elseExpr != null
                    ? collectColumns(elseExpr).stream()
                    : Stream.<String>empty();
                yield Stream.concat(whenColumns, elseColumns).distinct().toList();
            }
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

    static List<String> collectColumnsFromCondition(Condition condition) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, _) -> collectColumns(left, right);
            case Condition.IsNull(var attr, _) -> List.of(attr);
            case Condition.And(var operands) ->
                operands.stream()
                    .flatMap(cond -> collectColumnsFromCondition(cond).stream())
                    .distinct()
                    .toList();
            case Condition.Or(var operands) ->
                operands.stream()
                    .flatMap(cond -> collectColumnsFromCondition(cond).stream())
                    .distinct()
                    .toList();
            case Condition.Not(var operand) -> collectColumnsFromCondition(operand);
        };
    }

    static Expression toSqlCondition(Condition condition, String baseName) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var op) ->
                comparison(toSqlExpr(left, baseName), op, toSqlExpr(right, baseName));
            case Condition.IsNull(var attr, var negated) -> {
                var isNull = new net.sf.jsqlparser.expression.operators.relational.IsNullExpression();
                isNull.setLeftExpression(column(attrTable(baseName, attr), "v"));
                isNull.setNot(negated);
                yield isNull;
            }
            case Condition.And(var operands) ->
                andAll(operands.stream().map(cond -> paren(toSqlCondition(cond, baseName))).toList());
            case Condition.Or(var operands) ->
                orAll(operands.stream().map(cond -> paren(toSqlCondition(cond, baseName))).toList());
            case Condition.Not(var operand) ->
                not(paren(toSqlCondition(operand, baseName)));
        };
    }

    static boolean containsCaseWhen(IRExpression expr) {
        return switch (expr) {
            case IRExpression.CaseWhen _ -> true;
            case IRExpression.BinaryOp(var l, _, var r) -> containsCaseWhen(l) || containsCaseWhen(r);
            case IRExpression.Cast(var inner, _) -> containsCaseWhen(inner);
            case IRExpression.ColumnRef _, IRExpression.Literal _ -> false;
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ -> false;
        };
    }

    static Expression toSqlExpr(IRExpression expr, String baseName) {
        return switch (expr) {
            case IRExpression.ColumnRef(var col) -> column(attrTable(baseName, col), "v");
            case IRExpression.Literal lit -> literal(lit);
            case IRExpression.BinaryOp(var left, var op, var right) ->
                arithmetic(toSqlExpr(left, baseName), op, toSqlExpr(right, baseName));
            case IRExpression.Cast(var inner, var targetType) ->
                new CastExpression("CAST", toSqlExpr(inner, baseName), targetType);
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var sqlCaseExpr = new CaseExpression();
                var sqlWhens = whens.stream()
                    .map(when -> {
                        var sqlWhen = new net.sf.jsqlparser.expression.WhenClause();
                        sqlWhen.setWhenExpression(toSqlCondition(when.condition(), baseName));
                        sqlWhen.setThenExpression(toSqlExpr(when.result(), baseName));
                        return sqlWhen;
                    })
                    .toList();
                sqlCaseExpr.setWhenClauses(sqlWhens);
                if (elseExpr != null) {
                    sqlCaseExpr.setElseExpression(toSqlExpr(elseExpr, baseName));
                }
                yield sqlCaseExpr;
            }
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported expression in SQL rendering");
        };
    }
}
