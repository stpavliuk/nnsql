package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.util.Option;

import java.util.List;

final class Conditions {
    private Conditions() {
    }

    static List<Condition> conjunctionOperands(Condition condition) {
        return switch (condition) {
            case Condition.And(var operands) -> operands;
            default -> List.of(condition);
        };
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
