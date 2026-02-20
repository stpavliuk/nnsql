package nnsql.query.ir;

import java.util.List;

public record AggFilter(
    IRNode input,
    Condition condition,
    List<String> attributes
) implements IRNode {

    public static AggFilter aggFilter(IRNode input, Condition condition, List<String> attributes) {
        return new AggFilter(input, condition, attributes);
    }

    @Override
    public String toString() {
        return "AGG_FILTER(%s)".formatted(condition);
    }
}
