package nnsql.query.ir;

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
            var validFunctions = java.util.Set.of("SUM", "AVG", "COUNT", "MIN", "MAX");
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

    record ScalarSubquery(java.util.List<IRNode> subqueryPipeline) implements IRExpression {
        @Override
        public String toString() {
            return "(SUBQUERY)";
        }
    }

    record BinaryOp(IRExpression left, String operator, IRExpression right) implements IRExpression {
        public BinaryOp {
            var validOps = java.util.Set.of("+", "-", "*", "/");
            if (!validOps.contains(operator)) {
                throw new IllegalArgumentException("Unknown arithmetic operator: " + operator);
            }
        }

        @Override
        public String toString() {
            return "(%s %s %s)".formatted(left, operator, right);
        }
    }

    record Cast(IRExpression expr, String targetType) implements IRExpression {
        @Override
        public String toString() {
            return "CAST(%s AS %s)".formatted(expr, targetType);
        }
    }

    record WhenClause(Condition condition, IRExpression result) {}

    record CaseWhen(java.util.List<WhenClause> whens, IRExpression elseExpr) implements IRExpression {
        @Override
        public String toString() {
            var whensSql = whens.stream()
                .map(when -> "WHEN %s THEN %s".formatted(when.condition(), when.result()))
                .collect(java.util.stream.Collectors.joining(" "));
            var elseSql = elseExpr != null ? " ELSE %s".formatted(elseExpr) : "";
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
