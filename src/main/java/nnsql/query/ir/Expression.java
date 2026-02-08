package nnsql.query.ir;

public sealed interface Expression {

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

    record ColumnRef(String columnName) implements Expression {
        @Override
        public String toString() {
            return columnName;
        }
    }

    record Literal(Object value, LiteralType type) implements Expression {
        @Override
        public String toString() {
            return switch (type) {
                case NUMBER -> value.toString();
                case STRING -> "'" + value.toString() + "'";
                case NULL -> "NULL";
            };
        }
    }

    record Arithmetic(Expression left, String operator, Expression right) implements Expression {
        public Arithmetic {
            throw new UnsupportedOperationException(
                "Arithmetic expressions not yet implemented: " +
                left + " " + operator + " " + right);
        }

        @Override
        public String toString() {
            return "(%s %s %s)".formatted(left, operator, right);
        }
    }

    record Aggregate(String function, Expression argument, String alias) implements Expression {
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

        public static Aggregate agg(String function, Expression argument, String alias) {
            return new Aggregate(function.toUpperCase(), argument, alias);
        }
    }

    record ScalarSubquery(java.util.List<IRNode> subqueryPipeline) implements Expression {
        @Override
        public String toString() {
            return "(SUBQUERY)";
        }
    }
}
