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
    AtomicInteger pipelineCounter,
    List<String> attributes,
    SchemaRegistry schema,
    AtomicInteger nodeIds
) {

    public static IRPipeline from(List<Relation> relations, AtomicInteger nodeIds, SchemaRegistry schema) {
        var pipelineCounter = new AtomicInteger(0);
        var productNode = new Product(relations, pipelineCounter.getAndIncrement());
        var attributes = AttributeResolver.collectFromProduct(productNode);
        return new IRPipeline(Option.some(productNode), pipelineCounter, attributes, schema, nodeIds);
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

    public IRPipeline groupBy(SelectAnalysis analysis) {
        if (!analysis.hasGroupBy() && !analysis.hasAggregates()) return this;
        var groupingAttrs = analysis.resolveGroupByColumns(this.attributes);
        var aggregates = analysis.extractAggregates(this.attributes, this.schema, this.nodeIds);
        return group(groupingAttrs, aggregates);
    }

    public IRPipeline select(SelectAnalysis analysis) {
        if (analysis.star()) return returnAll();
        var refs = analysis.buildSelectRefs(this.attributes, this.schema, this.nodeIds);
        return returnSelected(refs);
    }

    public IRPipeline distinctIf(boolean shouldApply) {
        return shouldApply ? duplElim() : this;
    }

    public IRPipeline group(
        List<String> groupingAttributes,
        List<IRExpression.Aggregate> aggregates
    ) {
        var node = requireCurrent("Cannot group without a source");
        var outputAttrs = new ArrayList<String>();
        outputAttrs.addAll(groupingAttributes);
        outputAttrs.addAll(aggregates.stream().map(IRExpression.Aggregate::alias).toList());
        var groupNode = Group.of(node, groupingAttributes, aggregates, outputAttrs, pipelineCounter.getAndIncrement());
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

    // --- Internal helpers ---

    private IRNode requireCurrent(String message) {
        return switch (current) {
            case Some(IRNode node) -> node;
            case None() -> throw new IllegalStateException(message);
        };
    }

    private IRPipeline withNode(IRNode newCurrent, List<String> newAttributes) {
        return new IRPipeline(Option.some(newCurrent), pipelineCounter, newAttributes, schema, nodeIds);
    }
}
