package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.List;

final class Conditions {
    private Conditions() {
    }

    static List<Condition> conjunctionOperands(Condition condition) {
        var flattened = new ArrayList<Condition>();
        collectConjunctionOperands(condition, flattened);
        return List.copyOf(flattened);
    }

    private static void collectConjunctionOperands(Condition condition, List<Condition> operands) {
        switch (condition) {
            case Condition.And(var nestedOperands) ->
                nestedOperands.forEach(operand -> collectConjunctionOperands(operand, operands));
            default -> operands.add(condition);
        }
    }

    static Option<Condition> optionalConjunction(List<Condition> conditions) {
        return switch (conditions.size()) {
            case 0 -> Option.none();
            case 1 -> Option.some(conditions.getFirst());
            default -> Option.some(Condition.and(conditions));
        };
    }

    static Condition conjunction(List<Condition> conditions) {
        return switch (conditions.size()) {
            case 0 -> new Condition.And(List.of());
            case 1 -> conditions.getFirst();
            default -> Condition.and(conditions);
        };
    }
}
