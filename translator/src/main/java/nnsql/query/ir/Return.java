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

    public record AttributeRef(IRExpression source, String alias) {
        public AttributeRef(IRExpression source, String alias) {
            this.source = source;
            this.alias = alias != null ? alias : deriveAlias(source);
        }

        private static String deriveAlias(IRExpression source) {
            return switch (source) {
                case IRExpression.ColumnRef(var col) -> col;
                case IRExpression.Aggregate(_, _, var aggAlias) -> aggAlias;
                default -> throw new IllegalArgumentException("Expression requires an alias");
            };
        }

        public static AttributeRef attr(String sourceName, String alias) {
            return new AttributeRef(new IRExpression.ColumnRef(sourceName), alias);
        }

        public static AttributeRef attr(String sourceName) {
            return attr(sourceName, sourceName);
        }

        public static AttributeRef expr(IRExpression source, String alias) {
            return new AttributeRef(source, alias);
        }

        public boolean isSimpleColumn() {
            return source instanceof IRExpression.ColumnRef;
        }

        public String sourceColumnName() {
            return source instanceof IRExpression.ColumnRef(var col) ? col : null;
        }
    }
}
