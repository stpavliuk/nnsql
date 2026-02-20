package nnsql.query.builder;

import nnsql.query.ir.*;
import nnsql.query.ir.Return.AttributeRef;
import nnsql.util.Option;
import nnsql.util.Option.None;
import nnsql.util.Option.Some;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public record IRPipeline(Option<IRNode> current, AtomicInteger nodeIdCounter) {

    public IRPipeline product(List<Relation> relations) {
        var productNode = new Product(relations, nodeIdCounter.getAndIncrement());
        return withCurrent(productNode);
    }

    public IRPipeline filter(Condition condition, List<String> attributes) {
        return switch (current) {
            case Some(IRNode node) -> withCurrent(new Filter(node, condition, attributes));
            case None() -> throw new IllegalStateException("Cannot filter without a source");
        };
    }

    public IRPipeline filterIf(Condition condition, List<String> attributes) {
        return condition != null ? filter(condition, attributes) : this;
    }

    public IRPipeline group(
        List<String> groupingAttributes,
        List<IRExpression.Aggregate> aggregates,
        List<String> outputAttributes
    ) {
        return withCurrent(switch (current) {
            case Some(IRNode node) ->
                Group.of(node, groupingAttributes, aggregates, outputAttributes, nodeIdCounter.getAndIncrement());
            case None() -> throw new IllegalStateException("Cannot group without a source");
        });
    }

    public IRPipeline aggFilter(Condition condition, List<String> attributes) {
        return switch (current) {
            case Some(IRNode node) -> withCurrent(new AggFilter(node, condition, attributes));
            case None() -> throw new IllegalStateException("Cannot apply HAVING without a source");
        };
    }

    public IRPipeline returnAll() {
        return switch (current) {
            case Some(IRNode node) -> withCurrent(new Return(node, List.of(), true));
            case None() -> throw new IllegalStateException("Cannot return without a source");
        };
    }

    public IRPipeline returnSelected(List<AttributeRef> selectedAttributes) {
        return switch (current) {
            case Some(IRNode node) -> withCurrent(new Return(node, selectedAttributes, false));
            case None() -> throw new IllegalStateException("Cannot return without a source");
        };
    }

    public IRPipeline duplElim(List<String> attributes) {
        return switch (current) {
            case Some(IRNode node) -> withCurrent(new DuplElim(node, attributes));
            case None() -> throw new IllegalStateException("Cannot apply DISTINCT without a source");
        };
    }

    public IRPipeline duplElimIf(boolean shouldApply, List<String> attributes) {
        return shouldApply ? duplElim(attributes) : this;
    }

    public IRNode build() {
        return switch (current) {
            case Some(IRNode node) -> node;
            case None() -> throw new IllegalStateException("No IR node has been built");
        };
    }

    private IRPipeline withCurrent(IRNode newCurrent) {
        return new IRPipeline(Option.ofNullable(newCurrent), nodeIdCounter);
    }

    public static IRPipeline start() {
        return new IRPipeline(Option.none(), new AtomicInteger(0));
    }
}
