package nnsql.query.ir;

public sealed interface OutputAlias permits
    OutputAlias.Column,
    OutputAlias.Expression,
    OutputAlias.Aggregate {
    static OutputAlias column(String name) {
        return new Column(name);
    }

    static OutputAlias expression(int position) {
        return new Expression(position);
    }

    static OutputAlias aggregate(IRExpression.Aggregate aggregate) {
        return new Aggregate(aggregate.function(), aggregate.argument());
    }

    record Column(String name) implements OutputAlias {
        @Override
        public String toString() {
            return name;
        }
    }

    record Expression(int position) implements OutputAlias {
        @Override
        public String toString() {
            return "expr_" + position;
        }
    }

    record Aggregate(String function, IRExpression argument) implements OutputAlias {
        @Override
        public String toString() {
            return function.toLowerCase() + "_" + argument;
        }
    }
}
