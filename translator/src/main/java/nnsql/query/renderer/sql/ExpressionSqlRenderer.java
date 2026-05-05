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
            case IRExpression.FunctionCall(_, var arguments) -> arguments.stream()
                .flatMap(argument -> collectColumns(argument).stream())
                .distinct()
                .toList();
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var whenColumns = whens.stream()
                    .flatMap(when -> Stream.concat(
                        collectColumnsFromCondition(when.condition()).stream(),
                        collectColumns(when.result()).stream()
                    ));
                var elseColumns = elseExpr.stream()
                    .flatMap(elseValue -> collectColumns(elseValue).stream());
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
            case Condition.Like(var left, var pattern, _) -> collectColumns(left, pattern);
            case Condition.Exists _ -> List.of();
            case Condition.InSubquery(var left, _, _) -> collectColumns(left);
            case Condition.And(var operands) -> collectColumnsFromOperands(operands);
            case Condition.Or(var operands) -> collectColumnsFromOperands(operands);
            case Condition.Not(var operand) -> collectColumnsFromCondition(operand);
        };
    }

    private static List<String> collectColumnsFromOperands(List<Condition> operands) {
        return operands.stream()
            .flatMap(cond -> collectColumnsFromCondition(cond).stream())
            .distinct()
            .toList();
    }

    static Expression toSqlCondition(Condition condition, String baseName) {
        return toSqlCondition(condition, baseName, SqlDialect.duckDb());
    }

    static Expression toSqlCondition(Condition condition, String baseName, SqlDialect dialect) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var op) ->
                comparison(toSqlExpr(left, baseName, dialect), op, toSqlExpr(right, baseName, dialect));
            case Condition.IsNull(var attr, var negated) -> {
                var isNull = new net.sf.jsqlparser.expression.operators.relational.IsNullExpression();
                isNull.setLeftExpression(column(attrTable(baseName, attr), "v"));
                isNull.setNot(negated);
                yield isNull;
            }
            case Condition.Like(var left, var pattern, var negated) ->
                like(toSqlExpr(left, baseName, dialect), toSqlExpr(pattern, baseName, dialect), negated);
            case Condition.Exists _, Condition.InSubquery _ ->
                throw new UnsupportedOperationException(
                    "Subquery predicates are not supported in expression SQL rendering"
                );
            case Condition.And(var operands) ->
                andAll(operands.stream().map(cond -> paren(toSqlCondition(cond, baseName, dialect))).toList());
            case Condition.Or(var operands) ->
                orAll(operands.stream().map(cond -> paren(toSqlCondition(cond, baseName, dialect))).toList());
            case Condition.Not(var operand) ->
                not(paren(toSqlCondition(operand, baseName, dialect)));
        };
    }

    static boolean containsCaseWhen(IRExpression expr) {
        return switch (expr) {
            case IRExpression.CaseWhen _ -> true;
            case IRExpression.BinaryOp(var l, _, var r) -> containsCaseWhen(l) || containsCaseWhen(r);
            case IRExpression.Cast(var inner, _) -> containsCaseWhen(inner);
            case IRExpression.FunctionCall(_, var arguments) ->
                arguments.stream().anyMatch(ExpressionSqlRenderer::containsCaseWhen);
            case IRExpression.ColumnRef _, IRExpression.Literal _ -> false;
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ -> false;
        };
    }

    static Expression toSqlExpr(IRExpression expr, String baseName) {
        return toSqlExpr(expr, baseName, SqlDialect.duckDb());
    }

    static Expression toSqlExpr(IRExpression expr, String baseName, SqlDialect dialect) {
        return switch (expr) {
            case IRExpression.ColumnRef(var col) -> column(attrTable(baseName, col), "v");
            case IRExpression.Literal lit -> literal(lit);
            case IRExpression.BinaryOp binaryOp -> renderBinaryOp(binaryOp, baseName, dialect);
            case IRExpression.Cast(var inner, var targetType) ->
                new CastExpression("CAST", toSqlExpr(inner, baseName, dialect), targetType);
            case IRExpression.FunctionCall(var name, var arguments) ->
                dialect.renderFunction(
                    name,
                    renderFunctionArguments(name, arguments, baseName, dialect)
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var sqlCaseExpr = new CaseExpression();
                var sqlWhens = whens.stream()
                    .map(when -> {
                        var sqlWhen = new net.sf.jsqlparser.expression.WhenClause();
                        sqlWhen.setWhenExpression(toSqlCondition(when.condition(), baseName, dialect));
                        sqlWhen.setThenExpression(toSqlExpr(when.result(), baseName, dialect));
                        return sqlWhen;
                    })
                    .toList();
                sqlCaseExpr.setWhenClauses(sqlWhens);
                elseExpr
                    .map(elseValue -> toSqlExpr(elseValue, baseName, dialect))
                    .stream()
                    .forEach(sqlCaseExpr::setElseExpression);
                yield sqlCaseExpr;
            }
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported expression in SQL rendering");
        };
    }

    private static List<Expression> renderFunctionArguments(
        String functionName,
        List<IRExpression> arguments,
        String baseName,
        SqlDialect dialect
    ) {
        if (!isSubstringFunction(functionName)) {
            return arguments.stream()
                .map(argument -> toSqlExpr(argument, baseName, dialect))
                .toList();
        }

        return java.util.stream.IntStream.range(0, arguments.size())
            .mapToObj(index -> renderSubstringArgument(arguments.get(index), index, baseName, dialect))
            .toList();
    }

    private static Expression renderSubstringArgument(
        IRExpression argument,
        int index,
        String baseName,
        SqlDialect dialect
    ) {
        if (index == 0) {
            return toSqlExpr(argument, baseName, dialect);
        }

        return switch (argument) {
            case IRExpression.Literal(var value, var type)
                when type == IRExpression.LiteralType.NUMBER
                    && value instanceof Number number
                    && isWholeNumber(number) ->
                new LongValue(number.longValue());
            default -> toSqlExpr(argument, baseName, dialect);
        };
    }

    private static boolean isSubstringFunction(String functionName) {
        return "SUBSTR".equalsIgnoreCase(functionName)
            || "SUBSTRING".equalsIgnoreCase(functionName);
    }

    private static boolean isWholeNumber(Number number) {
        return switch (number) {
            case Integer _, Long _, Short _, Byte _ -> true;
            case Float floatValue -> Float.isFinite(floatValue) && floatValue == Math.rint(floatValue);
            case Double doubleValue -> Double.isFinite(doubleValue) && doubleValue == Math.rint(doubleValue);
            default -> {
                var decimal = new java.math.BigDecimal(number.toString());
                yield decimal.stripTrailingZeros().scale() <= 0;
            }
        };
    }

    private static Expression renderBinaryOp(IRExpression.BinaryOp binaryOp, String baseName, SqlDialect dialect) {
        var left = toSqlExpr(binaryOp.left(), baseName, dialect);
        var right = toSqlExpr(binaryOp.right(), baseName, dialect);

        if (needsParentheses(binaryOp.operator(), binaryOp.left(), false)) {
            left = paren(left);
        }
        if (needsParentheses(binaryOp.operator(), binaryOp.right(), true)) {
            right = paren(right);
        }

        return arithmetic(left, binaryOp.operator().toSql(), right);
    }

    private static boolean needsParentheses(IRExpression.ArithmeticOperator parentOperator,
                                            IRExpression childExpression,
                                            boolean isRightChild) {
        if (!(childExpression instanceof IRExpression.BinaryOp(_, var childOperator, _))) {
            return false;
        }

        var parentPrecedence = precedence(parentOperator);
        var childPrecedence = precedence(childOperator);

        if (childPrecedence < parentPrecedence) {
            return true;
        }
        if (childPrecedence > parentPrecedence || !isRightChild) {
            return false;
        }

        return switch (parentOperator) {
            case IRExpression.Add _ -> false;
            case IRExpression.Subtract _ -> true;
            case IRExpression.Multiply _ -> childOperator instanceof IRExpression.Divide;
            case IRExpression.Divide _ -> true;
        };
    }

    private static int precedence(IRExpression.ArithmeticOperator operator) {
        return switch (operator) {
            case IRExpression.Add _, IRExpression.Subtract _ -> 1;
            case IRExpression.Multiply _, IRExpression.Divide _ -> 2;
        };
    }
}
