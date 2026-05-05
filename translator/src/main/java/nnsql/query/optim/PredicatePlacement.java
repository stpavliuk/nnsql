package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record PredicatePlacement(
    List<JoinPredicate> joinPredicates,
    Map<ProductRelation, List<Condition>> localPredicates,
    Option<Condition> residual
) {
    PredicatePlacement {
        joinPredicates = List.copyOf(joinPredicates);
        localPredicates = copy(localPredicates);
    }

    static PredicatePlacement empty() {
        return new PredicatePlacement(List.of(), Map.of(), Option.none());
    }

    static PredicatePlacement join(JoinPredicate joinPredicate) {
        return new PredicatePlacement(List.of(joinPredicate), Map.of(), Option.none());
    }

    static PredicatePlacement local(ProductRelation relation, Condition condition) {
        return new PredicatePlacement(List.of(), Map.of(relation, List.of(condition)), Option.none());
    }

    static PredicatePlacement residual(Condition condition) {
        return new PredicatePlacement(List.of(), Map.of(), Option.some(condition));
    }

    PredicatePlacement merge(PredicatePlacement other) {
        var joins = new ArrayList<>(joinPredicates);
        joins.addAll(other.joinPredicates);

        var locals = new LinkedHashMap<>(localPredicates);
        other.localPredicates.forEach((relation, conditions) ->
            locals.merge(relation, conditions, PredicatePlacement::concat));

        return new PredicatePlacement(
            joins,
            locals,
            mergeResidual(residual, other.residual)
        );
    }

    private static Option<Condition> mergeResidual(
        Option<Condition> left,
        Option<Condition> right
    ) {
        return Conditions.optionalConjunction(
            java.util.stream.Stream.concat(left.stream(), right.stream()).toList()
        );
    }

    private static List<Condition> concat(List<Condition> left, List<Condition> right) {
        var result = new ArrayList<>(left);
        result.addAll(right);
        return result;
    }

    private static Map<ProductRelation, List<Condition>> copy(
        Map<ProductRelation, List<Condition>> localPredicates
    ) {
        var copy = new LinkedHashMap<ProductRelation, List<Condition>>();
        localPredicates.forEach((relation, conditions) -> copy.put(relation, List.copyOf(conditions)));
        return Map.copyOf(copy);
    }
}
