package nnsql.query.builder;

import nnsql.query.ir.*;
import nnsql.query.ir.Condition.*;
import nnsql.query.ir.Return.AttributeRef;
import nnsql.util.Option;

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
            case Like(var left, var pattern, var negated) ->
                new Like(
                    qualifyExpression(left, availableAttrs),
                    qualifyExpression(pattern, availableAttrs),
                    negated
                );
            case Exists(var subquery, var negated) -> new Exists(subquery, negated);
            case InSubquery(var left, var subquery, var negated) ->
                new InSubquery(
                    qualifyExpression(left, availableAttrs),
                    subquery,
                    negated
                );
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

    public static IRExpression qualifyExpression(IRExpression expr, List<String> availableAttrs) {
        return switch (expr) {
            case IRExpression.ColumnRef(var columnName) ->
                new IRExpression.ColumnRef(resolve(columnName, availableAttrs));
            case IRExpression.BinaryOp(var left, var op, var right) ->
                new IRExpression.BinaryOp(
                    qualifyExpression(left, availableAttrs),
                    op,
                    qualifyExpression(right, availableAttrs));
            case IRExpression.Cast(var inner, var targetType) ->
                new IRExpression.Cast(qualifyExpression(inner, availableAttrs), targetType);
            case IRExpression.FunctionCall(var name, var arguments) ->
                new IRExpression.FunctionCall(
                    name,
                    arguments.stream()
                        .map(argument -> qualifyExpression(argument, availableAttrs))
                        .toList()
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var qualifiedWhens = whens.stream()
                    .map(when -> new IRExpression.WhenClause(
                        qualifyCondition(when.condition(), availableAttrs),
                        qualifyExpression(when.result(), availableAttrs)
                    ))
                    .toList();
                var qualifiedElse = qualifyExpression(elseExpr, availableAttrs);
                yield new IRExpression.CaseWhen(qualifiedWhens, qualifiedElse);
            }
            case IRExpression.Literal _,
                 IRExpression.Aggregate _,
                 IRExpression.ScalarSubquery _ -> expr;
        };
    }

    private static Option<IRExpression> qualifyExpression(
        Option<IRExpression> expression, List<String> availableAttrs
    ) {
        return expression.map(expr -> qualifyExpression(expr, availableAttrs));
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
            case Sort sort -> collectFrom(sort.input());
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
