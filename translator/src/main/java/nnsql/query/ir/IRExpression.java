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
}
