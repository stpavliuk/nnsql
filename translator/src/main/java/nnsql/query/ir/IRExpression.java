package nnsql.query.ir;

import nnsql.util.Option;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public sealed interface IRExpression {

    enum LiteralType {
        NUMBER,   // integer or decimal
        STRING,   // string literal
        NULL      // NULL value
    }

    static ColumnRef col(String columnName) {
        return new ColumnRef(columnName);
    }

    static Literal number(Number value) {
        return new Literal(value, LiteralType.NUMBER);
    }

    static Literal string(String value) {
        return new Literal(value, LiteralType.STRING);
    }

    static Literal nullValue() {
        return new Literal(null, LiteralType.NULL);
    }

    static Literal literal(Object value, LiteralType type) {
        return new Literal(value, type);
    }

    record ColumnRef(String columnName) implements IRExpression {
        @Override
        public String toString() {
            return columnName;
        }
    }

    record Literal(Object value, LiteralType type) implements IRExpression {
        @Override
        public String toString() {
            return switch (type) {
                case NUMBER -> value.toString();
                case STRING -> "'" + value.toString() + "'";
                case NULL -> "NULL";
            };
        }
    }

    record Aggregate(String function, IRExpression argument, String alias) implements IRExpression {
        public Aggregate {
            var validFunctions = Set.of("SUM", "AVG", "COUNT", "MIN", "MAX");
            if (!validFunctions.contains(function.toUpperCase())) {
                throw new IllegalArgumentException("Unknown aggregate function: " + function);
            }
        }

        @Override
        public String toString() {
            return "%s(%s) AS %s".formatted(function, argument, alias);
        }

        public static Aggregate agg(String function, IRExpression argument, String alias) {
            return new Aggregate(function.toUpperCase(), argument, alias);
        }
    }

    record ScalarSubquery(List<IRNode> subqueryPipeline) implements IRExpression {
        @Override
        public String toString() {
            return "(SUBQUERY)";
        }
    }

    record FunctionCall(String name, List<IRExpression> arguments) implements IRExpression {
        public FunctionCall {
            name = Option.ofNullable(name)
                .map(String::strip)
                .flatMap(value -> value.isEmpty() ? Option.none() : Option.some(value))
                .orElseThrow(() -> new IllegalArgumentException("Function name must not be blank"));
            arguments = List.copyOf(arguments);
        }

        @Override
        public String toString() {
            var argsSql = arguments.stream()
                .map(IRExpression::toString)
                .collect(Collectors.joining(", "));
            return "%s(%s)".formatted(name, argsSql);
        }
    }

    sealed interface ArithmeticOperator permits Add, Subtract, Multiply, Divide {
        String symbol();

        static ArithmeticOperator fromSymbol(String symbol) {
            return switch (symbol) {
                case "+" -> new Add();
                case "-" -> new Subtract();
                case "*" -> new Multiply();
                case "/" -> new Divide();
                default -> throw new IllegalArgumentException("Unknown arithmetic operator: " + symbol);
            };
        }

        default String toSql() {
            return symbol();
        }
    }

    record Add() implements ArithmeticOperator {
        @Override
        public String symbol() {
            return "+";
        }
    }

    record Subtract() implements ArithmeticOperator {
        @Override
        public String symbol() {
            return "-";
        }
    }

    record Multiply() implements ArithmeticOperator {
        @Override
        public String symbol() {
            return "*";
        }
    }

    record Divide() implements ArithmeticOperator {
        @Override
        public String symbol() {
            return "/";
        }
    }

    record BinaryOp(IRExpression left, ArithmeticOperator operator, IRExpression right) implements IRExpression {
        public BinaryOp(IRExpression left, String operator, IRExpression right) {
            this(left, ArithmeticOperator.fromSymbol(operator), right);
        }

        @Override
        public String toString() {
            return "(%s %s %s)".formatted(left, operator.symbol(), right);
        }
    }

    record Cast(IRExpression expr, String targetType) implements IRExpression {
        @Override
        public String toString() {
            return "CAST(%s AS %s)".formatted(expr, targetType);
        }
    }

    record WhenClause(Condition condition, IRExpression result) {}

    record CaseWhen(List<WhenClause> whens, Option<IRExpression> elseExpr) implements IRExpression {
        @Override
        public String toString() {
            var whensSql = whens.stream()
                .map(when -> "WHEN %s THEN %s".formatted(when.condition(), when.result()))
                .collect(Collectors.joining(" "));
            var elseSql = elseExpr
                .map(expr -> " ELSE %s".formatted(expr))
                .orElse("");
            return "CASE %s%s END".formatted(whensSql, elseSql);
        }
    }

    static BinaryOp add(IRExpression left, IRExpression right) {
        return new BinaryOp(left, "+", right);
    }

    static BinaryOp subtract(IRExpression left, IRExpression right) {
        return new BinaryOp(left, "-", right);
    }

    static BinaryOp multiply(IRExpression left, IRExpression right) {
        return new BinaryOp(left, "*", right);
    }

    static BinaryOp divide(IRExpression left, IRExpression right) {
        return new BinaryOp(left, "/", right);
    }
}
