package nnsql.query.ir;

import java.util.List;

public record Group(
    IRNode input,
    List<String> groupingAttributes,
    List<Expression.Aggregate> aggregates,
    List<String> outputAttributes,
    int nodeId
) implements IRNode {

    public static Group of(IRNode input, List<String> groupingAttrs,
                           List<Expression.Aggregate> aggregates,
                           List<String> outputAttrs, int nodeId) {
        return new Group(input, groupingAttrs, aggregates, outputAttrs, nodeId);
    }

    @Override
    public String toString() {
        var groupByStr = groupingAttributes.isEmpty()
            ? ""
            : " GROUP BY " + String.join(", ", groupingAttributes);
        var aggStr = aggregates.isEmpty()
            ? ""
            : " AGGR " + aggregates.stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.joining(", "));
        return "GROUP" + groupByStr + aggStr;
    }
}
