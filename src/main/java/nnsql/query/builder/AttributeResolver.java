package nnsql.query.builder;

import nnsql.query.ir.*;
import nnsql.query.ir.Condition.*;
import nnsql.query.ir.Return.AttributeRef;

import java.util.List;

public class AttributeResolver {

    public static Condition qualifyCondition(Condition condition, List<String> availableAttrs) {
        return switch (condition) {
            case Comparison(var left, var right, var op) ->
                Condition.compare(
                    qualifyExpression(left, availableAttrs),
                    op,
                    qualifyExpression(right, availableAttrs)
                );

            case IsNull(var attr, var negated) -> new IsNull(resolve(attr, availableAttrs), negated);
            case And(var operands) -> {
                var qualifiedOperands = operands.stream()
                    .map(cond -> qualifyCondition(cond, availableAttrs))
                    .toList();

                yield Condition.and(qualifiedOperands);
            }
            case Or(var operands) -> {
                var qualifiedOperands = operands.stream()
                    .map(cond -> qualifyCondition(cond, availableAttrs))
                    .toList();

                yield Condition.or(qualifiedOperands);
            }
            case Not(var operand) -> Condition.not(qualifyCondition(operand, availableAttrs));
        };
    }

    private static IRExpression qualifyExpression(IRExpression expr, List<String> availableAttrs) {
        return switch (expr) {
            case IRExpression.ColumnRef(var columnName) ->
                new IRExpression.ColumnRef(resolve(columnName, availableAttrs));
            case IRExpression.Literal _,
                 IRExpression.Aggregate _,
                 IRExpression.ScalarSubquery _ -> expr;
        };
    }

    public static List<String> collectFromProduct(Product product) {
        return product.relations().stream()
            .flatMap(rel -> rel.attributes().stream()
                .map(attr -> rel.alias() + "_" + attr))
            .toList();
    }

    public static List<String> collectFrom(IRNode node) {
        return switch (node) {
            case Return ret when !ret.selectStar() ->
                ret.selectedAttributes().stream().map(AttributeRef::alias).toList();
            case Return ret -> collectFrom(ret.input());
            case DuplElim de -> de.attributes();
            case AggFilter af -> af.attributes();
            case Group g -> g.outputAttributes();
            case Filter f -> f.attributes();
            case Product p -> collectFromProduct(p);
        };
    }

    public static String resolve(String attrName, List<String> availableAttrs) {
        if (availableAttrs.contains(attrName)) {
            return attrName;
        }

        var matches = availableAttrs.stream()
            .filter(attr -> attr.endsWith("_" + attrName))
            .findAny();

        return matches.orElseThrow(() -> new IllegalArgumentException(
            "Attribute '%s' not found in available attributes: %s"
                .formatted(attrName, String.join(", ", availableAttrs))));
    }
}
