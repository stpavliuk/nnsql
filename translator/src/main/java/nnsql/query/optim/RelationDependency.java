package nnsql.query.optim;

import nnsql.util.Option;

import static nnsql.util.Option.None;
import static nnsql.util.Option.Some;

record RelationDependency(State state, Option<ProductRelation> relation) {

    enum State {
        INDEPENDENT,
        SINGLE_RELATION,
        UNKNOWN
    }

    static RelationDependency independent() {
        return new RelationDependency(State.INDEPENDENT, Option.none());
    }

    static RelationDependency single(ProductRelation relation) {
        return new RelationDependency(State.SINGLE_RELATION, Option.some(relation));
    }

    static RelationDependency unknown() {
        return new RelationDependency(State.UNKNOWN, Option.none());
    }

    RelationDependency merge(RelationDependency other) {
        return switch (state) {
            case UNKNOWN -> unknown();
            case INDEPENDENT -> other;
            case SINGLE_RELATION -> switch (relation) {
                case Some(var left) -> mergeSingle(left, other);
                case None<ProductRelation> _ -> unknown();
            };
        };
    }

    Option<ProductRelation> localRelation() {
        return switch (state) {
            case SINGLE_RELATION -> relation;
            case INDEPENDENT, UNKNOWN -> Option.none();
        };
    }

    private RelationDependency mergeSingle(ProductRelation left, RelationDependency other) {
        return switch (other.state) {
            case INDEPENDENT -> this;
            case SINGLE_RELATION -> mergeSingleRelations(left, other.relation);
            case UNKNOWN -> unknown();
        };
    }

    private RelationDependency mergeSingleRelations(
        ProductRelation left,
        Option<ProductRelation> rightRelation
    ) {
        return switch (rightRelation) {
            case Some(var right) when left.position() == right.position() -> this;
            case Some<ProductRelation> _, None<ProductRelation> _ -> unknown();
        };
    }
}
