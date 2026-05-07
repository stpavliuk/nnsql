package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.query.ir.Filter;
import nnsql.query.ir.IRExpression;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.ir.Return;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinPredicatePushdownTest {

    @Test
    void extractsEquiJoinPredicateAndKeepsRemainingFilter() {
        var product = product();
        var crossRelationPredicate = Condition.gt(IRExpression.col("a_value"), IRExpression.col("b_value"));
        var filter = new Filter(
            product,
            Condition.and(joinPredicate(), crossRelationPredicate),
            List.of("a_id", "a_value", "b_id", "b_value")
        );

        var optimized = JoinPredicatePushdown.optimize(filter);

        var remainingFilter = assertInstanceOf(Filter.class, optimized);
        var optimizedProduct = assertInstanceOf(Product.class, remainingFilter.input());

        assertEquals(List.of(new JoinPredicate(0, "id", 1, "id")), optimizedProduct.joinPredicates());
        assertEquals(crossRelationPredicate, remainingFilter.condition());
    }

    @Test
    void pushesSingleRelationPredicateIntoTableRelation() {
        var product = product();
        var localPredicate = Condition.gt(IRExpression.col("a_value"), IRExpression.number(10));
        var filter = new Filter(
            product,
            Condition.and(joinPredicate(), localPredicate),
            List.of("a_id", "a_value", "b_id", "b_value")
        );

        var optimized = JoinPredicatePushdown.optimize(filter);

        var optimizedProduct = assertInstanceOf(Product.class, optimized);
        var pushedRelation = assertInstanceOf(Relation.Subquery.class, optimizedProduct.relations().getFirst());
        var relationProjection = assertInstanceOf(Return.class, pushedRelation.ir());
        var relationFilter = assertInstanceOf(Filter.class, relationProjection.input());

        assertEquals(List.of(new JoinPredicate(0, "id", 1, "id")), optimizedProduct.joinPredicates());
        assertEquals(localPredicate, relationFilter.condition());
        assertEquals(List.of("id", "value"), pushedRelation.attributes());
    }

    @Test
    void pushesSingleRelationDisjunctionIntoTableRelation() {
        var product = product();
        var localPredicate = Condition.or(
            Condition.eq(IRExpression.col("a_value"), IRExpression.number(10)),
            Condition.eq(IRExpression.col("a_value"), IRExpression.number(20))
        );
        var filter = new Filter(
            product,
            Condition.and(joinPredicate(), localPredicate),
            List.of("a_id", "a_value", "b_id", "b_value")
        );

        var optimized = JoinPredicatePushdown.optimize(filter);

        var optimizedProduct = assertInstanceOf(Product.class, optimized);
        var pushedRelation = assertInstanceOf(Relation.Subquery.class, optimizedProduct.relations().getFirst());
        var relationProjection = assertInstanceOf(Return.class, pushedRelation.ir());
        var relationFilter = assertInstanceOf(Filter.class, relationProjection.input());

        assertEquals(localPredicate, relationFilter.condition());
    }

    @Test
    void recursivelyAnalyzesNestedConjunctionsForLocalPushdown() {
        var product = product();
        var localPredicate = Condition.or(
            Condition.eq(IRExpression.col("a_value"), IRExpression.number(10)),
            Condition.eq(IRExpression.col("a_value"), IRExpression.number(20))
        );
        var residualPredicate = Condition.gt(IRExpression.col("a_value"), IRExpression.col("b_value"));
        var filter = new Filter(
            product,
            Condition.and(joinPredicate(), Condition.and(localPredicate, residualPredicate)),
            List.of("a_id", "a_value", "b_id", "b_value")
        );

        var optimized = JoinPredicatePushdown.optimize(filter);

        var remainingFilter = assertInstanceOf(Filter.class, optimized);
        var optimizedProduct = assertInstanceOf(Product.class, remainingFilter.input());
        var pushedRelation = assertInstanceOf(Relation.Subquery.class, optimizedProduct.relations().getFirst());
        var relationProjection = assertInstanceOf(Return.class, pushedRelation.ir());
        var relationFilter = assertInstanceOf(Filter.class, relationProjection.input());

        assertEquals(localPredicate, relationFilter.condition());
        assertEquals(residualPredicate, remainingFilter.condition());
    }

    @Test
    void factorsCommonJoinPredicateOutOfOrBranchesBeforeExtraction() {
        var product = product();
        var leftLocalPredicate = Condition.gt(IRExpression.col("a_value"), IRExpression.number(10));
        var rightLocalPredicate = Condition.lt(IRExpression.col("b_value"), IRExpression.number(20));
        var filter = new Filter(
            product,
            Condition.or(
                Condition.and(joinPredicate(), leftLocalPredicate),
                Condition.and(joinPredicate(), rightLocalPredicate)
            ),
            List.of("a_id", "a_value", "b_id", "b_value")
        );

        var optimized = JoinPredicatePushdown.optimize(filter);

        var remainingFilter = assertInstanceOf(Filter.class, optimized);
        var optimizedProduct = assertInstanceOf(Product.class, remainingFilter.input());

        assertEquals(List.of(new JoinPredicate(0, "id", 1, "id")), optimizedProduct.joinPredicates());
        assertEquals(Condition.or(leftLocalPredicate, rightLocalPredicate), remainingFilter.condition());
    }

    @Test
    void preservesExistingJoinPredicatesWhenNoNewJoinPredicateIsExtracted() {
        var existingJoinPredicates = List.of(new JoinPredicate(0, "id", 1, "id"));
        var product = new Product(product().relations(), 1, existingJoinPredicates);
        var crossRelationPredicate = Condition.gt(IRExpression.col("a_value"), IRExpression.col("b_value"));
        var filter = new Filter(
            product,
            crossRelationPredicate,
            List.of("a_id", "a_value", "b_id", "b_value")
        );

        var optimized = JoinPredicatePushdown.optimize(filter);

        var remainingFilter = assertInstanceOf(Filter.class, optimized);
        var optimizedProduct = assertInstanceOf(Product.class, remainingFilter.input());

        assertEquals(existingJoinPredicates, optimizedProduct.joinPredicates());
        assertEquals(crossRelationPredicate, remainingFilter.condition());
    }

    @Test
    void doesNotPushPredicateIntoReservedSubqueryRelation() {
        var subqueryRelation = Relation.subquery(
            "a",
            new Return(
                new Product(List.of(Relation.table("A", "inner_a", List.of("id", "value"))), 10),
                List.of(
                    Return.AttributeRef.attr("inner_a_id", "id"),
                    Return.AttributeRef.attr("inner_a_value", "value")
                ),
                false
            ),
            List.of("id", "value")
        );
        var product = new Product(List.of(
            subqueryRelation,
            Relation.table("B", "b", List.of("id", "value"))
        ), 1);
        var localPredicate = Condition.gt(IRExpression.col("a_value"), IRExpression.number(10));
        var filter = new Filter(product, localPredicate, List.of("a_id", "a_value", "b_id", "b_value"));

        var optimized = JoinPredicatePushdown.optimize(filter);

        var remainingFilter = assertInstanceOf(Filter.class, optimized);
        var optimizedProduct = assertInstanceOf(Product.class, remainingFilter.input());

        assertEquals(product.relations(), optimizedProduct.relations());
        assertTrue(optimizedProduct.joinPredicates().isEmpty());
        assertEquals(localPredicate, remainingFilter.condition());
    }

    private static Product product() {
        return new Product(List.of(
            Relation.table("A", "a", List.of("id", "value")),
            Relation.table("B", "b", List.of("id", "value"))
        ), 1);
    }

    private static Condition joinPredicate() {
        return Condition.eq(IRExpression.col("a_id"), IRExpression.col("b_id"));
    }
}
