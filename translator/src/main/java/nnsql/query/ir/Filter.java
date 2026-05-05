package nnsql.query.ir;

import java.util.List;

public record Filter(IRNode input, Condition condition, List<String> attributes) implements IRNode {

    public Filter(IRNode input, Condition condition, List<String> attributes) {
        this.input = input;
        this.condition = condition;
        this.attributes = List.copyOf(attributes);
    }

    public Filter withInput(IRNode newInput) {
        return new Filter(newInput, condition, attributes);
    }

    public Filter withInputAndCondition(IRNode newInput, Condition newCondition) {
        return new Filter(newInput, newCondition, attributes);
    }

    @Override
    public String toString() {
        return "FILTER(%s)".formatted(condition);
    }
}
