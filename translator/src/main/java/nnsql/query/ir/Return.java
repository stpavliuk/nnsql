package nnsql.query.ir;

import nnsql.util.Option;

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

    public sealed interface AttributeRef
        permits ColumnAttributeRef, ExpressionAttributeRef {
        IRExpression source();

        String alias();

        static AttributeRef attr(String sourceName, String alias) {
            var source = new IRExpression.ColumnRef(sourceName);
            var resolvedAlias = Option.ofNullable(alias).orElse(sourceName);
            return new ColumnAttributeRef(source, resolvedAlias);
        }

        static AttributeRef attr(String sourceName) {
            return attr(sourceName, sourceName);
        }

        static AttributeRef expr(IRExpression source, String alias) {
            var resolvedAlias = Option.ofNullable(alias).orElseGet(() -> deriveAlias(source));
            return switch (source) {
                case IRExpression.ColumnRef col -> new ColumnAttributeRef(col, resolvedAlias);
                default -> new ExpressionAttributeRef(source, resolvedAlias);
            };
        }

        private static String deriveAlias(IRExpression source) {
            return switch (source) {
                case IRExpression.ColumnRef(var col) -> col;
                case IRExpression.Aggregate(_, _, var aggAlias, _) -> aggAlias;
                default -> throw new IllegalArgumentException("Expression requires an alias");
            };
        }
    }

    public record ColumnAttributeRef(IRExpression.ColumnRef source, String alias) implements AttributeRef {
    }

    public record ExpressionAttributeRef(IRExpression source, String alias) implements AttributeRef {
        public ExpressionAttributeRef {
            if (source instanceof IRExpression.ColumnRef) {
                throw new IllegalArgumentException("Column expressions must use ColumnAttributeRef");
            }
        }
    }
}
