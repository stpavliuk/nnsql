package nnsql.query.optim;

import nnsql.query.ir.AggFilter;
import nnsql.query.ir.Condition;
import nnsql.query.ir.DuplElim;
import nnsql.query.ir.Filter;
import nnsql.query.ir.Group;
import nnsql.query.ir.IRExpression;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.ir.Return;
import nnsql.query.ir.Sort;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JoinPredicatePushdown {
    private JoinPredicatePushdown() {
    }

    public static IRNode optimize(IRNode node) {
        return optimize(node, Scope.root());
    }

    private static IRNode optimize(IRNode node, Scope scope) {
        return switch (node) {
            case Product product -> optimizeProduct(product, scope);
            case Filter filter -> optimizeFilter(filter, scope);
            case Group group -> optimizeGroup(group, scope);
            case AggFilter aggFilter -> optimizeAggFilter(aggFilter, scope);
            case Return ret -> optimizeReturn(ret, scope);
            case DuplElim duplElim -> new DuplElim(optimize(duplElim.input(), scope), duplElim.attributes());
            case Sort sort -> new Sort(optimize(sort.input(), scope), sort.keys(), sort.limit());
        };
    }

    private static Product optimizeProduct(Product product, Scope scope) {
        return product.withRelations(product.relations().stream()
            .map(relation -> optimizeRelation(relation, scope.withReservedAliases(product.relations())))
            .toList());
    }

    private static Relation optimizeRelation(Relation relation, Scope scope) {
        return switch (relation) {
            case Relation.Subquery(var alias, var ir, var attributes) ->
                Relation.subquery(alias, optimize(ir, scope.allowLocalPushdown()), attributes);
            case Relation.Table table -> table;
        };
    }

    private static IRNode optimizeFilter(Filter filter, Scope scope) {
        var optimizedFilter = filter.withInputAndCondition(
            optimize(filter.input(), scope),
            optimizeCondition(filter.condition(), scope)
        );

        return switch (optimizedFilter.input()) {
            case Product product when product.relations().size() >= 2 ->
                new ProductFilterPushdown(
                    optimizedFilter,
                    product,
                    scope.allowLocalPredicatePushdown(),
                    scope.reservedRelationAliases()
                ).optimize();
            default -> optimizedFilter;
        };
    }

    private static Group optimizeGroup(Group group, Scope scope) {
        return new Group(
            optimize(group.input(), scope),
            group.groupingAttributes(),
            group.aggregates().stream()
                .map(aggregate -> optimizeAggregate(aggregate, scope))
                .toList(),
            group.outputAttributes(),
            group.nodeId()
        );
    }

    private static AggFilter optimizeAggFilter(AggFilter aggFilter, Scope scope) {
        return new AggFilter(
            optimize(aggFilter.input(), scope),
            optimizeCondition(aggFilter.condition(), scope),
            aggFilter.attributes()
        );
    }

    private static Return optimizeReturn(Return ret, Scope scope) {
        return new Return(
            optimize(ret.input(), scope),
            ret.selectedAttributes().stream()
                .map(attribute -> optimizeAttribute(attribute, scope))
                .toList(),
            ret.selectStar()
        );
    }

    private static Return.AttributeRef optimizeAttribute(Return.AttributeRef attribute, Scope scope) {
        return switch (attribute) {
            case Return.ColumnAttributeRef columnAttribute -> columnAttribute;
            case Return.ExpressionAttributeRef expressionAttribute ->
                Return.AttributeRef.expr(
                    optimizeExpression(expressionAttribute.source(), scope),
                    expressionAttribute.alias()
                );
        };
    }

    private static Condition optimizeCondition(Condition condition, Scope scope) {
        return switch (condition) {
            case Condition.Comparison comparison -> optimizeComparison(comparison, scope);
            case Condition.IsNull isNull -> isNull;
            case Condition.Like like -> optimizeLike(like, scope);
            case Condition.Exists exists -> optimizeExists(exists, scope);
            case Condition.InSubquery inSubquery -> optimizeInSubquery(inSubquery, scope);
            case Condition.And(var operands) -> Condition.and(optimizeConditions(operands, scope));
            case Condition.Or(var operands) -> Condition.or(optimizeConditions(operands, scope));
            case Condition.Not(var operand) -> Condition.not(optimizeCondition(operand, scope));
        };
    }

    private static Condition optimizeComparison(Condition.Comparison comparison, Scope scope) {
        return Condition.compare(
            optimizeExpression(comparison.left(), scope),
            comparison.operator(),
            optimizeExpression(comparison.right(), scope)
        );
    }

    private static Condition.Like optimizeLike(Condition.Like like, Scope scope) {
        return new Condition.Like(
            optimizeExpression(like.left(), scope),
            optimizeExpression(like.pattern(), scope),
            like.isNegated()
        );
    }

    private static Condition.Exists optimizeExists(Condition.Exists exists, Scope scope) {
        return new Condition.Exists(
            optimize(exists.subquery(), scope.disableLocalPushdown()),
            exists.isNegated(),
            exists.correlations()
        );
    }

    private static Condition.InSubquery optimizeInSubquery(Condition.InSubquery inSubquery, Scope scope) {
        return new Condition.InSubquery(
            optimizeExpression(inSubquery.left(), scope),
            optimize(inSubquery.subquery(), scope.disableLocalPushdown()),
            inSubquery.isNegated()
        );
    }

    private static List<Condition> optimizeConditions(List<Condition> conditions, Scope scope) {
        return conditions.stream()
            .map(condition -> optimizeCondition(condition, scope))
            .toList();
    }

    private static IRExpression optimizeExpression(IRExpression expression, Scope scope) {
        return switch (expression) {
            case IRExpression.ColumnRef columnRef -> columnRef;
            case IRExpression.Literal literal -> literal;
            case IRExpression.Aggregate aggregate -> optimizeAggregate(aggregate, scope);
            case IRExpression.BinaryOp binaryOp -> optimizeBinaryOp(binaryOp, scope);
            case IRExpression.Cast cast -> optimizeCast(cast, scope);
            case IRExpression.FunctionCall functionCall -> optimizeFunctionCall(functionCall, scope);
            case IRExpression.CaseWhen caseWhen -> optimizeCaseWhen(caseWhen, scope);
            case IRExpression.ScalarSubquery scalarSubquery -> optimizeScalarSubquery(scalarSubquery, scope);
        };
    }

    private static IRExpression.Aggregate optimizeAggregate(IRExpression.Aggregate aggregate, Scope scope) {
        return new IRExpression.Aggregate(
            aggregate.function(),
            optimizeExpression(aggregate.argument(), scope),
            aggregate.alias(),
            aggregate.distinct()
        );
    }

    private static IRExpression.BinaryOp optimizeBinaryOp(IRExpression.BinaryOp binaryOp, Scope scope) {
        return new IRExpression.BinaryOp(
            optimizeExpression(binaryOp.left(), scope),
            binaryOp.operator(),
            optimizeExpression(binaryOp.right(), scope)
        );
    }

    private static IRExpression.Cast optimizeCast(IRExpression.Cast cast, Scope scope) {
        return new IRExpression.Cast(
            optimizeExpression(cast.expr(), scope),
            cast.targetType()
        );
    }

    private static IRExpression.FunctionCall optimizeFunctionCall(IRExpression.FunctionCall functionCall, Scope scope) {
        return new IRExpression.FunctionCall(
            functionCall.name(),
            functionCall.arguments().stream()
                .map(argument -> optimizeExpression(argument, scope))
                .toList()
        );
    }

    private static IRExpression.CaseWhen optimizeCaseWhen(IRExpression.CaseWhen caseWhen, Scope scope) {
        return new IRExpression.CaseWhen(
            caseWhen.whens().stream()
                .map(when -> new IRExpression.WhenClause(
                    optimizeCondition(when.condition(), scope),
                    optimizeExpression(when.result(), scope)
                ))
                .toList(),
            caseWhen.elseExpr().map(expression -> optimizeExpression(expression, scope))
        );
    }

    private static IRExpression.ScalarSubquery optimizeScalarSubquery(
        IRExpression.ScalarSubquery scalarSubquery,
        Scope scope
    ) {
        return new IRExpression.ScalarSubquery(
            scalarSubquery.subqueryPipeline().stream()
                .map(node -> optimize(node, scope.disableLocalPushdown()))
                .toList(),
            scalarSubquery.correlations(),
            scalarSubquery.valueAttribute()
        );
    }

    private record Scope(
        boolean allowLocalPredicatePushdown,
        Set<String> reservedRelationAliases
    ) {
        static Scope root() {
            return new Scope(true, Set.of());
        }

        Scope allowLocalPushdown() {
            return new Scope(true, reservedRelationAliases);
        }

        Scope disableLocalPushdown() {
            return new Scope(false, reservedRelationAliases);
        }

        Scope withReservedAliases(List<Relation> relations) {
            var aliases = new LinkedHashSet<>(reservedRelationAliases);
            relations.stream()
                .map(Relation::alias)
                .forEach(aliases::add);
            return new Scope(allowLocalPredicatePushdown, Set.copyOf(aliases));
        }
    }
}
