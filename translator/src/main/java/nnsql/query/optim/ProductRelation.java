package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.query.ir.Filter;
import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.ir.Return;

import java.util.List;

record ProductRelation(int position, Relation relation) {

    String alias() {
        return relation.alias();
    }

    List<String> attributes() {
        return relation.attributes();
    }

    boolean isTable() {
        return switch (relation) {
            case Relation.Table _ -> true;
            case Relation.Subquery _ -> false;
        };
    }

    Relation withLocalFilter(Condition condition, int productNodeId) {
        var relationProduct = new Product(List.of(relation), derivedNodeId(productNodeId));
        var filteredAttributes = attributes().stream()
            .map(attribute -> alias() + "_" + attribute)
            .toList();
        var filtered = new Filter(relationProduct, condition, filteredAttributes);
        var projection = new Return(
            filtered,
            attributes().stream()
                .map(attribute -> Return.AttributeRef.attr(alias() + "_" + attribute, attribute))
                .toList(),
            false
        );
        return Relation.subquery(alias(), projection, attributes());
    }

    private int derivedNodeId(int productNodeId) {
        return 1_000_000 + (productNodeId * 100) + position;
    }
}
