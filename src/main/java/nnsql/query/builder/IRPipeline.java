package nnsql.query.builder;

import nnsql.query.SchemaRegistry;
import nnsql.query.ir.*;
import nnsql.query.ir.Condition;
import nnsql.query.ir.Return.AttributeRef;
import nnsql.util.Option;
import nnsql.util.Option.None;
import nnsql.util.Option.Some;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public record IRPipeline(
    Option<IRNode> current,
    AtomicInteger nodeIdCounter,
    List<String> attributes,
    SchemaRegistry schema
) {

    public static IRPipeline from(List<Relation> relations, SchemaRegistry schema) {
        var counter = new AtomicInteger(0);
        var productNode = new Product(relations, counter.getAndIncrement());
        var attributes = AttributeResolver.collectFromProduct(productNode);
        return new IRPipeline(Option.some(productNode), counter, attributes, schema);
    }

    public IRPipeline filter(Option<Condition> condition) {
        return switch (condition) {
            case None() -> this;
            case Some(var cond) -> {
                var node = requireCurrent("Cannot filter without a source");
                var qualifiedCondition = AttributeResolver.qualifyCondition(cond, this.attributes);
                yield withNode(new Filter(node, qualifiedCondition, this.attributes), this.attributes);
            }
        };
    }

    public IRPipeline aggFilter(Option<Condition> condition) {
        return switch (condition) {
            case None() -> this;
            case Some(var cond) -> {
                var node = requireCurrent("Cannot apply HAVING without a source");
                var qualifiedCondition = AttributeResolver.qualifyCondition(cond, this.attributes);
                yield withNode(new AggFilter(node, qualifiedCondition, this.attributes), this.attributes);
            }
        };
    }

    public IRPipeline group(
        List<String> groupingAttributes,
        List<IRExpression.Aggregate> aggregates
    ) {
        var node = requireCurrent("Cannot group without a source");
        var outputAttrs = new ArrayList<String>();
        outputAttrs.addAll(groupingAttributes);
        outputAttrs.addAll(aggregates.stream().map(IRExpression.Aggregate::alias).toList());
        var groupNode = Group.of(node, groupingAttributes, aggregates, outputAttrs, nodeIdCounter.getAndIncrement());
        return withNode(groupNode, outputAttrs);
    }

    public IRPipeline returnAll() {
        var node = requireCurrent("Cannot return without a source");
        return withNode(new Return(node, List.of(), true), this.attributes);
    }

    public IRPipeline returnSelected(List<AttributeRef> selectedAttributes) {
        var node = requireCurrent("Cannot return without a source");
        var newAttrs = selectedAttributes.stream().map(AttributeRef::alias).toList();
        return withNode(new Return(node, selectedAttributes, false), newAttrs);
    }

    public IRPipeline duplElim() {
        var node = requireCurrent("Cannot apply DISTINCT without a source");
        return withNode(new DuplElim(node, this.attributes), this.attributes);
    }

    public IRNode build() {
        return switch (current) {
            case Some(IRNode node) -> node;
            case None() -> throw new IllegalStateException("No IR node has been built");
        };
    }

    // --- Legacy methods (kept for backward compatibility during refactor) ---

    public IRPipeline product(List<Relation> relations) {
        var productNode = new Product(relations, nodeIdCounter.getAndIncrement());
        var attrs = AttributeResolver.collectFromProduct(productNode);
        return withNode(productNode, attrs);
    }

    public IRPipeline filter(Condition condition, List<String> attrs) {
        var node = requireCurrent("Cannot filter without a source");
        return withNode(new Filter(node, condition, attrs), attrs);
    }

    public IRPipeline aggFilter(Condition condition, List<String> attrs) {
        var node = requireCurrent("Cannot apply HAVING without a source");
        return withNode(new AggFilter(node, condition, attrs), attrs);
    }

    public IRPipeline group(
        List<String> groupingAttributes,
        List<IRExpression.Aggregate> aggregates,
        List<String> outputAttributes
    ) {
        var node = requireCurrent("Cannot group without a source");
        var groupNode = Group.of(node, groupingAttributes, aggregates, outputAttributes, nodeIdCounter.getAndIncrement());
        return withNode(groupNode, outputAttributes);
    }

    public IRPipeline duplElim(List<String> attrs) {
        var node = requireCurrent("Cannot apply DISTINCT without a source");
        return withNode(new DuplElim(node, attrs), attrs);
    }

    public static IRPipeline start() {
        return new IRPipeline(Option.none(), new AtomicInteger(0), List.of(), null);
    }

    // --- Internal helpers ---

    private IRNode requireCurrent(String message) {
        return switch (current) {
            case Some(IRNode node) -> node;
            case None() -> throw new IllegalStateException(message);
        };
    }

    private IRPipeline withNode(IRNode newCurrent, List<String> newAttributes) {
        return new IRPipeline(Option.some(newCurrent), nodeIdCounter, newAttributes, schema);
    }
}
