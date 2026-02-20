package nnsql.query.ir;

import java.util.List;

public record DuplElim(IRNode input, List<String> attributes) implements IRNode {
    public DuplElim(IRNode input, List<String> attributes) {
        this.input = input;
        this.attributes = List.copyOf(attributes);
    }

    @Override
    public String toString() {
        return "DUPL_ELIM()";
    }

}
