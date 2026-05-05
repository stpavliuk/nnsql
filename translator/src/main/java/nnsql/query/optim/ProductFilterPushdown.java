package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.query.ir.Filter;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static nnsql.util.Option.None;
import static nnsql.util.Option.Some;

final class ProductFilterPushdown {
    private final Filter filter;
    private final Product product;
    private final boolean allowLocalPredicatePushdown;
    private final Set<String> reservedRelationAliases;
    private final ProductScope scope;

    ProductFilterPushdown(
        Filter filter,
        Product product,
        boolean allowLocalPredicatePushdown,
        Set<String> reservedRelationAliases
    ) {
        this.filter = filter;
        this.product = product;
        this.allowLocalPredicatePushdown = allowLocalPredicatePushdown;
        this.reservedRelationAliases = Set.copyOf(reservedRelationAliases);
        this.scope = new ProductScope(product);
    }

    IRNode optimize() {
        var placement = new PredicatePlacementAnalyzer(
            scope,
            allowLocalPredicatePushdown,
            reservedRelationAliases
        ).analyze(filter.condition());
        var joinOptimizedProduct = placement.joinPredicates().isEmpty()
            ? product
            : product.withJoinPredicates(placement.joinPredicates());
        var finalProduct = joinOptimizedProduct.withRelations(relationsWithLocalFilters(placement));

        return filteredOrProduct(finalProduct, placement.residual());
    }

    private List<Relation> relationsWithLocalFilters(PredicatePlacement placement) {
        var relations = new ArrayList<Relation>();
        for (var relation : scope.relations()) {
            relations.add(relationWithLocalFilters(relation, placement));
        }
        return relations;
    }

    private Relation relationWithLocalFilters(
        ProductRelation relation,
        PredicatePlacement placement
    ) {
        var localPredicates = placement.localPredicates().getOrDefault(relation, List.of());
        return localPredicates.isEmpty()
            ? relation.relation()
            : relation.withLocalFilter(Conditions.conjunction(localPredicates), scope.nodeId());
    }

    private IRNode filteredOrProduct(Product input, Option<Condition> condition) {
        return switch (condition) {
            case None<Condition> _ -> input;
            case Some(var remainingCondition) when input.equals(filter.input()) && remainingCondition.equals(filter.condition()) ->
                filter.withInput(input);
            case Some(var remainingCondition) ->
                filter.withInputAndCondition(input, remainingCondition);
        };
    }
}
