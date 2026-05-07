package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.query.ir.IRExpression;
import nnsql.util.Option;

import java.util.List;
import java.util.stream.Stream;

final class PredicateDependencyAnalyzer {
    private final ProductScope scope;

    PredicateDependencyAnalyzer(ProductScope scope) {
        this.scope = scope;
    }

    RelationDependency find(Condition condition) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, _) -> merge(
                find(left),
                find(right)
            );
            case Condition.IsNull(var attrName, _) ->
                relationDependency(attrName);
            case Condition.Like(var left, var pattern, _) -> merge(
                find(left),
                find(pattern)
            );
            case Condition.And(var operands) -> merge(operands.stream().map(this::find).toList());
            case Condition.Or(var operands) -> merge(operands.stream().map(this::find).toList());
            case Condition.Not(var operand) -> find(operand);
            case Condition.Exists _, Condition.InSubquery _ -> RelationDependency.unknown();
        };
    }

    RelationDependency find(IRExpression expression) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) ->
                relationDependency(columnName);
            case IRExpression.Literal _ -> RelationDependency.independent();
            case IRExpression.BinaryOp(var left, _, var right) -> merge(
                find(left),
                find(right)
            );
            case IRExpression.Cast(var expr, _) -> find(expr);
            case IRExpression.FunctionCall(_, var arguments) ->
                merge(arguments.stream().map(this::find).toList());
            case IRExpression.CaseWhen(var whens, var elseExpr) ->
                findCaseWhen(whens, elseExpr);
            case IRExpression.ScalarSubquery _, IRExpression.Aggregate _ -> RelationDependency.unknown();
        };
    }

    private RelationDependency findCaseWhen(
        List<IRExpression.WhenClause> whens,
        Option<IRExpression> elseExpr
    ) {
        return merge(Stream.concat(
            whens.stream().flatMap(when -> Stream.of(
                find(when.condition()),
                find(when.result())
            )),
            elseExpr.stream().map(this::find)
        ).toList());
    }

    private RelationDependency relationDependency(String attributeName) {
        return scope.resolve(attributeName)
            .map(attribute -> RelationDependency.single(attribute.relation()))
            .orElseGet(RelationDependency::unknown);
    }

    private RelationDependency merge(RelationDependency left, RelationDependency right) {
        return left.merge(right);
    }

    private RelationDependency merge(List<RelationDependency> dependencies) {
        var current = RelationDependency.independent();
        for (var dependency : dependencies) {
            current = merge(current, dependency);
        }
        return current;
    }
}
