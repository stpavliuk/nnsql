package nnsql.query.ir;

import java.util.List;
import java.util.stream.Collectors;

public sealed interface Condition {

    static Comparison compare(IRExpression left, String operator, IRExpression right) {
        return new Comparison(left, right, operator);
    }

    static Comparison eq(IRExpression left, IRExpression right) {
        return new Comparison(left, right, "=");
    }

    static Comparison neq(IRExpression left, IRExpression right) {
        return new Comparison(left, right, "!=");
    }

    static Comparison lt(IRExpression left, IRExpression right) {
        return new Comparison(left, right, "<");
    }

    static Comparison gt(IRExpression left, IRExpression right) {
        return new Comparison(left, right, ">");
    }

    static Comparison lte(IRExpression left, IRExpression right) {
        return new Comparison(left, right, "<=");
    }

    static Comparison gte(IRExpression left, IRExpression right) {
        return new Comparison(left, right, ">=");
    }

    static IsNull isNull(String attrName) {
        return new IsNull(attrName, false);
    }

    static IsNull isNotNull(String attrName) {
        return new IsNull(attrName, true);
    }

    static And and(Condition... operands) {
        return new And(List.of(operands));
    }

    static And and(List<Condition> operands) {
        return new And(operands);
    }

    static Or or(Condition... operands) {
        return new Or(List.of(operands));
    }

    static Or or(List<Condition> operands) {
        return new Or(operands);
    }

    static Not not(Condition operand) {
        return new Not(operand);
    }

    record Comparison(IRExpression left, IRExpression right, String operator) implements Condition {
        @Override
        public String toString() {
            return "%s %s %s".formatted(left, operator, right);
        }
    }

    record IsNull(String attrName, boolean isNegated) implements Condition {
        @Override
        public String toString() {
            return isNegated ? "%s IS NOT NULL".formatted(attrName)
                             : "%s IS NULL".formatted(attrName);
        }
    }

    record And(List<Condition> operands) implements Condition {
        @Override
        public String toString() {
            return operands.stream()
                .map(cond -> "(" + cond.toString() + ")")
                .collect(Collectors.joining(" AND "));
        }
    }

    record Or(List<Condition> operands) implements Condition {
        @Override
        public String toString() {
            return operands.stream()
                .map(cond -> "(" + cond.toString() + ")")
                .collect(Collectors.joining(" OR "));
        }
    }

    record Not(Condition operand) implements Condition {
        @Override
        public String toString() {
            return "NOT (" + operand.toString() + ")";
        }
    }
}
