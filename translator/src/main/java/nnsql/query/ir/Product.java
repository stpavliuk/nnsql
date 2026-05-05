package nnsql.query.ir;

import nnsql.query.optim.JoinPredicate;

import java.util.List;
import java.util.stream.Collectors;

public record Product(
    List<Relation> relations,
    int nodeId,
    List<JoinPredicate> joinPredicates
) implements IRNode {

    public Product(List<Relation> relations, int nodeId) {
        this(relations, nodeId, List.of());
    }

    public Product {
        relations = List.copyOf(relations);
        joinPredicates = List.copyOf(joinPredicates);
    }

    public Product withRelations(List<Relation> newRelations) {
        return relations.equals(newRelations)
            ? this
            : new Product(newRelations, nodeId, joinPredicates);
    }

    public Product withJoinPredicates(List<JoinPredicate> newJoinPredicates) {
        return joinPredicates.equals(newJoinPredicates)
            ? this
            : new Product(relations, nodeId, newJoinPredicates);
    }

    @Override
    public String toString() {
        var relationNames = relations.stream()
            .map(Relation::alias)
            .collect(Collectors.joining(", "));
        return "PRODUCT(%s)".formatted(relationNames);
    }
}
