package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.query.ir.AggFilter;
import nnsql.query.ir.DuplElim;
import nnsql.query.ir.Filter;
import nnsql.query.ir.Group;
import nnsql.query.ir.IRExpression;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.ir.Return;
import nnsql.query.ir.Sort;
import nnsql.util.Option;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            case Condition.InSubquery(var left, var subquery, var negated)
                when !negated && !referencesScope(subquery, aliasesDeclaredIn(subquery)) ->
                find(left);
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

    private boolean referencesScope(IRNode node, Set<String> shadowedAliases) {
        return switch (node) {
            case Product product -> {
                var productShadowedAliases = new HashSet<>(shadowedAliases);
                product.relations().stream()
                    .map(Relation::alias)
                    .forEach(productShadowedAliases::add);
                yield product.relations().stream()
                    .anyMatch(relation -> referencesScope(relation, productShadowedAliases));
            }
            case Filter filter ->
                referencesScope(filter.input(), shadowedAliases)
                    || referencesScope(filter.condition(), shadowedAliases);
            case Group group -> referencesScope(group.input(), shadowedAliases)
                || group.groupingAttributes().stream().anyMatch(attribute -> referencesScope(attribute, shadowedAliases))
                || group.aggregates().stream().anyMatch(aggregate -> referencesScope(aggregate, shadowedAliases));
            case AggFilter aggFilter ->
                referencesScope(aggFilter.input(), shadowedAliases)
                    || referencesScope(aggFilter.condition(), shadowedAliases);
            case Return ret -> referencesScope(ret.input(), shadowedAliases)
                || ret.selectedAttributes().stream()
                    .map(Return.AttributeRef::source)
                    .anyMatch(expression -> referencesScope(expression, shadowedAliases));
            case DuplElim duplElim -> referencesScope(duplElim.input(), shadowedAliases)
                || duplElim.attributes().stream().anyMatch(attribute -> referencesScope(attribute, shadowedAliases));
            case Sort sort -> referencesScope(sort.input(), shadowedAliases)
                || sort.keys().stream()
                    .map(Sort.SortKey::attribute)
                    .anyMatch(attribute -> referencesScope(attribute, shadowedAliases));
        };
    }

    private boolean referencesScope(Relation relation, Set<String> shadowedAliases) {
        return switch (relation) {
            case Relation.Table _ -> false;
            case Relation.Subquery(_, var ir, _) -> referencesScope(ir, shadowedAliases);
        };
    }

    private boolean referencesScope(Condition condition, Set<String> shadowedAliases) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, _) ->
                referencesScope(left, shadowedAliases) || referencesScope(right, shadowedAliases);
            case Condition.IsNull(var attrName, _) -> referencesScope(attrName, shadowedAliases);
            case Condition.Like(var left, var pattern, _) ->
                referencesScope(left, shadowedAliases) || referencesScope(pattern, shadowedAliases);
            case Condition.Exists(var subquery, _, _) -> referencesScope(subquery, shadowedAliases);
            case Condition.InSubquery(var left, var subquery, _) ->
                referencesScope(left, shadowedAliases) || referencesScope(subquery, shadowedAliases);
            case Condition.And(var operands) ->
                operands.stream().anyMatch(operand -> referencesScope(operand, shadowedAliases));
            case Condition.Or(var operands) ->
                operands.stream().anyMatch(operand -> referencesScope(operand, shadowedAliases));
            case Condition.Not(var operand) -> referencesScope(operand, shadowedAliases);
        };
    }

    private boolean referencesScope(IRExpression expression, Set<String> shadowedAliases) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) -> referencesScope(columnName, shadowedAliases);
            case IRExpression.Literal _ -> false;
            case IRExpression.Aggregate aggregate -> referencesScope(aggregate, shadowedAliases);
            case IRExpression.BinaryOp(var left, _, var right) ->
                referencesScope(left, shadowedAliases) || referencesScope(right, shadowedAliases);
            case IRExpression.Cast(var expr, _) -> referencesScope(expr, shadowedAliases);
            case IRExpression.FunctionCall(_, var arguments) ->
                arguments.stream().anyMatch(argument -> referencesScope(argument, shadowedAliases));
            case IRExpression.CaseWhen(var whens, var elseExpr) ->
                whens.stream().anyMatch(when ->
                    referencesScope(when.condition(), shadowedAliases)
                        || referencesScope(when.result(), shadowedAliases))
                    || elseExpr.stream().anyMatch(expressionValue -> referencesScope(expressionValue, shadowedAliases));
            case IRExpression.ScalarSubquery(var pipeline, var correlations, _) ->
                pipeline.stream().anyMatch(node -> referencesScope(node, shadowedAliases))
                    || correlations.stream().anyMatch(correlation ->
                    referencesScope(correlation.outerAttribute(), shadowedAliases)
                        || referencesScope(correlation.innerAttribute(), shadowedAliases));
        };
    }

    private boolean referencesScope(IRExpression.Aggregate aggregate, Set<String> shadowedAliases) {
        return referencesScope(aggregate.argument(), shadowedAliases);
    }

    private boolean referencesScope(String attributeName, Set<String> shadowedAliases) {
        if (shadowedAliases.stream().anyMatch(alias -> attributeName.startsWith(alias + "_"))) {
            return false;
        }
        return scope.resolve(attributeName).isSome();
    }

    private Set<String> aliasesDeclaredIn(IRNode node) {
        var aliases = new HashSet<String>();
        collectAliases(node, aliases);
        return aliases;
    }

    private void collectAliases(IRNode node, Set<String> aliases) {
        switch (node) {
            case Product product -> product.relations().forEach(relation -> collectAliases(relation, aliases));
            case Filter filter -> collectAliases(filter.input(), aliases);
            case Group group -> collectAliases(group.input(), aliases);
            case AggFilter aggFilter -> collectAliases(aggFilter.input(), aliases);
            case Return ret -> collectAliases(ret.input(), aliases);
            case DuplElim duplElim -> collectAliases(duplElim.input(), aliases);
            case Sort sort -> collectAliases(sort.input(), aliases);
        }
    }

    private void collectAliases(Relation relation, Set<String> aliases) {
        aliases.add(relation.alias());
        if (relation instanceof Relation.Subquery(_, var ir, _)) {
            collectAliases(ir, aliases);
        }
    }
}
