package nnsql.query.optim;

import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.util.Option;
import nnsql.util.Streams;

import java.util.List;
import java.util.stream.IntStream;

record ProductScope(Product product) {

    List<ProductRelation> relations() {
        var relations = product.relations();
        return IntStream.range(0, relations.size())
            .mapToObj(position -> new ProductRelation(position, relations.get(position)))
            .toList();
    }

    List<Relation> rawRelations() {
        return product.relations();
    }

    int nodeId() {
        return product.nodeId();
    }

    Option<Attribute> resolve(String qualifiedAttribute) {
        return Streams.firstPresent(relations().stream()
            .map(relation -> resolve(qualifiedAttribute, relation)));
    }

    private Option<Attribute> resolve(String qualifiedAttribute, ProductRelation relation) {
        var prefix = relation.alias() + "_";
        if (!qualifiedAttribute.startsWith(prefix)) {
            return Option.none();
        }

        var rawAttribute = qualifiedAttribute.substring(prefix.length());
        return relation.attributes().contains(rawAttribute)
            ? Option.some(new Attribute(relation, rawAttribute))
            : Option.none();
    }

    record Attribute(ProductRelation relation, String name) {
    }
}
