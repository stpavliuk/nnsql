package nnsql.query.ir;

import java.util.List;
import java.util.stream.Collectors;

public record Return(IRNode input, List<AttributeRef> selectedAttributes, boolean selectStar) implements IRNode {

    public Return(IRNode input, List<AttributeRef> selectedAttributes, boolean selectStar) {
        this.input = input;
        this.selectedAttributes = List.copyOf(selectedAttributes);
        this.selectStar = selectStar;
    }

    @Override
    public String toString() {
        if (selectStar) {
            return "RETURN(*)";
        }
        var attrs = selectedAttributes.stream()
            .map(AttributeRef::alias)
            .collect(Collectors.joining(", "));
        return "RETURN(%s)".formatted(attrs);
    }

    public record AttributeRef(String sourceName, String alias) {
        public AttributeRef(String sourceName, String alias) {
            this.sourceName = sourceName;
            this.alias = alias != null ? alias : sourceName;
        }

        public static AttributeRef attr(String sourceName, String alias) {
            return new AttributeRef(sourceName, alias);
        }

        public static AttributeRef attr(String sourceName) {
            return new AttributeRef(sourceName, sourceName);
        }
    }
}
