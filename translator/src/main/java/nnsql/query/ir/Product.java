package nnsql.query.ir;

import java.util.List;
import java.util.stream.Collectors;

public record Product(List<Relation> relations, int nodeId) implements IRNode {

    public Product(List<Relation> relations, int nodeId) {
        this.relations = List.copyOf(relations);
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        var relationNames = relations.stream()
            .map(Relation::alias)
            .collect(Collectors.joining(", "));
        return "PRODUCT(%s)".formatted(relationNames);
    }
}
