package nnsql.query.optim;

import nnsql.query.ir.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JoinPredicatePushdown {

    public static IRNode optimize(IRNode node) {
        return optimize(node, true);
    }

    private static IRNode optimize(IRNode node, boolean allowLocalPredicatePushdown) {
        return switch (node) {
            case Product p -> optimizeProduct(p);
            case Filter f -> optimizeFilter(new Filter(
                optimize(f.input(), allowLocalPredicatePushdown),
                optimizeCondition(f.condition(), allowLocalPredicatePushdown),
                f.attributes()
            ), allowLocalPredicatePushdown);
            case Group g -> new Group(
                optimize(g.input(), allowLocalPredicatePushdown), g.groupingAttributes(),
                g.aggregates().stream()
                    .map(aggregate -> optimizeAggregate(aggregate, allowLocalPredicatePushdown))
                    .toList(),
                g.outputAttributes(), g.nodeId());
            case AggFilter af -> new AggFilter(
                optimize(af.input(), allowLocalPredicatePushdown),
                optimizeCondition(af.condition(), allowLocalPredicatePushdown),
                af.attributes());
            case Return r -> new Return(
                optimize(r.input(), allowLocalPredicatePushdown),
                optimizeSelectedAttributes(r.selectedAttributes(), allowLocalPredicatePushdown),
                r.selectStar());
            case DuplElim d -> new DuplElim(optimize(d.input(), allowLocalPredicatePushdown), d.attributes());
            case Sort s -> new Sort(optimize(s.input(), allowLocalPredicatePushdown), s.keys(), s.limit());
        };
    }

    private static Product optimizeProduct(Product p) {
        var newRelations = p.relations().stream()
            .map(r -> switch (r) {
                case Relation.Subquery(var alias, var ir, var attrs) ->
                    Relation.subquery(alias, optimize(ir, false), attrs);
                case Relation.Table t -> (Relation) t;
            })
            .toList();
        return new Product(newRelations, p.nodeId(), p.joinPredicates());
    }

    private static List<Return.AttributeRef> optimizeSelectedAttributes(
        List<Return.AttributeRef> attributes,
        boolean allowLocalPredicatePushdown
    ) {
        return attributes.stream()
            .map(attr -> switch (attr) {
                case Return.ColumnAttributeRef columnAttr -> (Return.AttributeRef) columnAttr;
                case Return.ExpressionAttributeRef expressionAttr ->
                    Return.AttributeRef.expr(
                        optimizeExpression(expressionAttr.source(), allowLocalPredicatePushdown),
                        expressionAttr.alias()
                    );
            })
            .toList();
    }

    private static IRExpression.Aggregate optimizeAggregate(
        IRExpression.Aggregate aggregate,
        boolean allowLocalPredicatePushdown
    ) {
        return new IRExpression.Aggregate(
            aggregate.function(),
            optimizeExpression(aggregate.argument(), allowLocalPredicatePushdown),
            aggregate.alias(),
            aggregate.distinct()
        );
    }

    private static Condition optimizeCondition(Condition condition, boolean allowLocalPredicatePushdown) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var operator) ->
                Condition.compare(
                    optimizeExpression(left, allowLocalPredicatePushdown),
                    operator,
                    optimizeExpression(right, allowLocalPredicatePushdown)
                );
            case Condition.IsNull isNull -> isNull;
            case Condition.Like(var left, var pattern, var isNegated) ->
                new Condition.Like(
                    optimizeExpression(left, allowLocalPredicatePushdown),
                    optimizeExpression(pattern, allowLocalPredicatePushdown),
                    isNegated
                );
            case Condition.Exists(var subquery, var isNegated, var correlations) ->
                new Condition.Exists(optimize(subquery, false), isNegated, correlations);
            case Condition.InSubquery(var left, var subquery, var isNegated) ->
                new Condition.InSubquery(
                    optimizeExpression(left, allowLocalPredicatePushdown),
                    optimize(subquery, false),
                    isNegated
                );
            case Condition.And(var operands) ->
                Condition.and(operands.stream()
                    .map(operand -> optimizeCondition(operand, allowLocalPredicatePushdown))
                    .toList());
            case Condition.Or(var operands) ->
                Condition.or(operands.stream()
                    .map(operand -> optimizeCondition(operand, allowLocalPredicatePushdown))
                    .toList());
            case Condition.Not(var operand) ->
                Condition.not(optimizeCondition(operand, allowLocalPredicatePushdown));
        };
    }

    private static IRExpression optimizeExpression(
        IRExpression expression,
        boolean allowLocalPredicatePushdown
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef columnRef -> columnRef;
            case IRExpression.Literal literal -> literal;
            case IRExpression.Aggregate aggregate -> optimizeAggregate(aggregate, allowLocalPredicatePushdown);
            case IRExpression.BinaryOp(var left, var operator, var right) ->
                new IRExpression.BinaryOp(
                    optimizeExpression(left, allowLocalPredicatePushdown),
                    operator,
                    optimizeExpression(right, allowLocalPredicatePushdown)
                );
            case IRExpression.Cast(var expr, var targetType) ->
                new IRExpression.Cast(optimizeExpression(expr, allowLocalPredicatePushdown), targetType);
            case IRExpression.FunctionCall(var name, var arguments) ->
                new IRExpression.FunctionCall(
                    name,
                    arguments.stream()
                        .map(argument -> optimizeExpression(argument, allowLocalPredicatePushdown))
                        .toList()
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) ->
                new IRExpression.CaseWhen(
                    whens.stream()
                        .map(when -> new IRExpression.WhenClause(
                            optimizeCondition(when.condition(), allowLocalPredicatePushdown),
                            optimizeExpression(when.result(), allowLocalPredicatePushdown)
                        ))
                        .toList(),
                    elseExpr.map(expr -> optimizeExpression(expr, allowLocalPredicatePushdown))
                );
            case IRExpression.ScalarSubquery(var subqueryPipeline, var correlations, var valueAttribute) ->
                new IRExpression.ScalarSubquery(
                    subqueryPipeline.stream().map(node -> optimize(node, false)).toList(),
                    correlations,
                    valueAttribute
                );
        };
    }

    private static IRNode optimizeFilter(Filter filter, boolean allowLocalPredicatePushdown) {
        var optimizedInput = optimize(filter.input(), allowLocalPredicatePushdown);

        if (!(optimizedInput instanceof Product product) || product.relations().size() < 2) {
            return new Filter(optimizedInput, filter.condition(), filter.attributes());
        }

        var normalized = factorOutCommonJoinPredicates(filter.condition(), product);
        var extraction = extractJoinPredicates(normalized, product);

        var joinOptimizedProduct = extraction.joinPredicates().isEmpty()
            ? product
            : new Product(product.relations(), product.nodeId(), extraction.joinPredicates());

        if (!allowLocalPredicatePushdown) {
            if (extraction.remainingCondition().isEmpty()) {
                return joinOptimizedProduct;
            }
            if (joinOptimizedProduct == optimizedInput
                && extraction.remainingCondition().get().equals(filter.condition())) {
                return new Filter(optimizedInput, filter.condition(), filter.attributes());
            }
            return new Filter(joinOptimizedProduct, extraction.remainingCondition().get(), filter.attributes());
        }

        var localExtraction = extraction.remainingCondition()
            .map(condition -> extractLocalPredicates(condition, joinOptimizedProduct))
            .orElseGet(() -> new LocalPredicateExtraction(joinOptimizedProduct.relations(), Optional.empty()));

        var finalProduct = localExtraction.relations().equals(joinOptimizedProduct.relations())
            ? joinOptimizedProduct
            : new Product(localExtraction.relations(), joinOptimizedProduct.nodeId(), joinOptimizedProduct.joinPredicates());

        if (localExtraction.remainingCondition().isEmpty()) {
            return finalProduct;
        }

        if (finalProduct == optimizedInput && localExtraction.remainingCondition().get().equals(filter.condition())) {
            return new Filter(optimizedInput, filter.condition(), filter.attributes());
        }

        return new Filter(finalProduct, localExtraction.remainingCondition().get(), filter.attributes());
    }

    /**
     * For OR conditions where every branch shares common equi-join predicates,
     * factor them out: OR(AND(J, A), AND(J, B)) → AND(J, OR(A, B))
     */
    private static Condition factorOutCommonJoinPredicates(Condition condition, Product product) {
        List<Condition> topOperands = switch (condition) {
            case Condition.And(var ops) -> ops;
            default -> List.of(condition);
        };

        var factored = new ArrayList<Condition>();
        boolean changed = false;

        for (var operand : topOperands) {
            if (operand instanceof Condition.Or(var orBranches) && orBranches.size() >= 2) {
                var result = factorOrBranches(orBranches, product);
                if (result.isPresent()) {
                    factored.addAll(result.get());
                    changed = true;
                    continue;
                }
            }
            factored.add(operand);
        }

        if (!changed) {
            return condition;
        }

        return factored.size() == 1 ? factored.getFirst() : Condition.and(factored);
    }

    /**
     * Given OR branches that are all ANDs, find equi-join predicates common to every branch,
     * extract them, and return AND(common..., OR(remainders...)).
     */
    private static Optional<List<Condition>> factorOrBranches(
        List<Condition> orBranches, Product product
    ) {
        var branchOperands = orBranches.stream()
            .map(branch -> switch (branch) {
                case Condition.And(var ops) -> ops;
                default -> List.of(branch);
            })
            .toList();

        // Find equi-join predicates present in the first branch
        var firstBranchJoins = branchOperands.getFirst().stream()
            .filter(c -> tryExtractJoinPredicate(c, product).isPresent())
            .collect(Collectors.toSet());

        if (firstBranchJoins.isEmpty()) {
            return Optional.empty();
        }

        // Keep only those present in ALL branches
        var commonJoins = firstBranchJoins.stream()
            .filter(jp -> branchOperands.stream().skip(1).allMatch(branch -> branch.contains(jp)))
            .toList();

        if (commonJoins.isEmpty()) {
            return Optional.empty();
        }

        var commonSet = Set.copyOf(commonJoins);

        // Build reduced OR branches (each branch minus the common predicates)
        var reducedBranches = branchOperands.stream()
            .map(ops -> ops.stream().filter(c -> !commonSet.contains(c)).toList())
            .map(ops -> switch (ops.size()) {
                case 0 -> (Condition) new Condition.And(List.of());
                case 1 -> ops.getFirst();
                default -> Condition.and(ops);
            })
            .toList();

        var result = new ArrayList<>(commonJoins);
        result.add(Condition.or(reducedBranches));
        return Optional.of(result);
    }

    private record ExtractionResult(
        List<JoinPredicate> joinPredicates,
        Optional<Condition> remainingCondition
    ) {
    }

    private record LocalPredicateExtraction(
        List<Relation> relations,
        Optional<Condition> remainingCondition
    ) {
    }

    private static ExtractionResult extractJoinPredicates(Condition condition, Product product) {
        var joinPredicates = new ArrayList<JoinPredicate>();
        var remaining = new ArrayList<Condition>();

        List<Condition> operands = switch (condition) {
            case Condition.And(var ops) -> ops;
            default -> List.of(condition);
        };

        for (var operand : operands) {
            tryExtractJoinPredicate(operand, product)
                .map(joinPredicates::add)
                .orElseGet(() -> remaining.add(operand));
        }

        Optional<Condition> remainingCondition = switch (remaining.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(remaining.getFirst());
            default -> Optional.of(Condition.and(remaining));
        };

        return new ExtractionResult(joinPredicates, remainingCondition);
    }

    private static LocalPredicateExtraction extractLocalPredicates(Condition condition, Product product) {
        var perRelationConditions = new ArrayList<List<Condition>>();
        for (int i = 0; i < product.relations().size(); i++) {
            perRelationConditions.add(new ArrayList<>());
        }

        var remaining = new ArrayList<Condition>();
        List<Condition> operands = switch (condition) {
            case Condition.And(var ops) -> ops;
            default -> List.of(condition);
        };

        for (var operand : operands) {
            findSingleRelationIndex(operand, product)
                .filter(relIndex -> product.relations().get(relIndex) instanceof Relation.Table)
                .ifPresentOrElse(
                    relIndex -> perRelationConditions.get(relIndex).add(operand),
                    () -> {
                        inferOrBranchLocalPredicates(operand, product).forEach(
                            (relIndex, inferred) -> perRelationConditions.get(relIndex).add(inferred)
                        );
                        remaining.add(operand);
                    }
                );
        }

        var relations = new ArrayList<Relation>();
        var changed = false;
        for (int i = 0; i < product.relations().size(); i++) {
            var relation = product.relations().get(i);
            var localConditions = perRelationConditions.get(i);
            if (localConditions.isEmpty()) {
                relations.add(relation);
                continue;
            }

            changed = true;
            relations.add(wrapRelationWithFilter(
                relation,
                localConditions.size() == 1 ? localConditions.getFirst() : Condition.and(localConditions),
                derivedNodeId(product.nodeId(), i)
            ));
        }

        if (!changed) {
            return new LocalPredicateExtraction(product.relations(), Optional.of(condition));
        }

        Optional<Condition> remainingCondition = switch (remaining.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(remaining.getFirst());
            default -> Optional.of(Condition.and(remaining));
        };
        return new LocalPredicateExtraction(relations, remainingCondition);
    }

    private static java.util.Map<Integer, Condition> inferOrBranchLocalPredicates(Condition condition, Product product) {
        if (!(condition instanceof Condition.Or(var branches))) {
            return java.util.Map.of();
        }

        var branchOperands = branches.stream()
            .map(branch -> switch (branch) {
                case Condition.And(var ops) -> ops;
                default -> List.of(branch);
            })
            .toList();

        var inferred = new LinkedHashMap<Integer, Condition>();
        for (int relIndex = 0; relIndex < product.relations().size(); relIndex++) {
            var currentRelIndex = relIndex;
            if (!(product.relations().get(relIndex) instanceof Relation.Table)) {
                continue;
            }

            var branchLocalConditions = new ArrayList<Condition>();
            var inferredForRelation = true;
            for (var branch : branchOperands) {
                var localOperands = branch.stream()
                    .filter(operand -> findSingleRelationIndex(operand, product).equals(Optional.of(currentRelIndex)))
                    .toList();
                if (localOperands.isEmpty()) {
                    inferredForRelation = false;
                    break;
                }
                branchLocalConditions.add(localOperands.size() == 1
                    ? localOperands.getFirst()
                    : Condition.and(localOperands));
            }

            if (inferredForRelation) {
                inferred.put(currentRelIndex, branchLocalConditions.size() == 1
                    ? branchLocalConditions.getFirst()
                    : Condition.or(branchLocalConditions));
            }
        }

        return inferred;
    }

    private static Relation wrapRelationWithFilter(Relation relation, Condition condition, int nodeId) {
        var relationProduct = new Product(List.of(relation), nodeId);
        var filteredAttributes = relation.attributes().stream()
            .map(attr -> relation.alias() + "_" + attr)
            .toList();
        var filtered = new Filter(relationProduct, condition, filteredAttributes);
        var projection = new Return(
            filtered,
            relation.attributes().stream()
                .map(attr -> Return.AttributeRef.attr(relation.alias() + "_" + attr, attr))
                .toList(),
            false
        );
        return Relation.subquery(relation.alias(), projection, relation.attributes());
    }

    private static int derivedNodeId(int productNodeId, int relationIndex) {
        return 1_000_000 + (productNodeId * 100) + relationIndex;
    }

    private static Optional<JoinPredicate> tryExtractJoinPredicate(
        Condition condition, Product product
    ) {
        if (!(condition instanceof Condition.Comparison(var left, var right, var operator))) {
            return Optional.empty();
        }
        if (!"=".equals(operator)) {
            return Optional.empty();
        }
        if (!(left instanceof IRExpression.ColumnRef(var leftCol))
            || !(right instanceof IRExpression.ColumnRef(var rightCol))) {
            return Optional.empty();
        }

        var leftRelInfo = findRelation(leftCol, product);
        var rightRelInfo = findRelation(rightCol, product);

        if (leftRelInfo.isEmpty() || rightRelInfo.isEmpty()) {
            return Optional.empty();
        }

        var leftInfo = leftRelInfo.get();
        var rightInfo = rightRelInfo.get();

        if (leftInfo.relIndex() == rightInfo.relIndex()) {
            return Optional.empty();
        }

        return Optional.of(new JoinPredicate(
            leftInfo.relIndex(), leftInfo.rawAttr(),
            rightInfo.relIndex(), rightInfo.rawAttr()
        ));
    }

    private static Optional<Integer> findSingleRelationIndex(Condition condition, Product product) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, _) -> mergeRelationIndices(
                findSingleRelationIndex(left, product),
                findSingleRelationIndex(right, product)
            );
            case Condition.IsNull(var attrName, _) -> findRelation(attrName, product).map(RelInfo::relIndex);
            case Condition.Like(var left, var pattern, _) -> mergeRelationIndices(
                findSingleRelationIndex(left, product),
                findSingleRelationIndex(pattern, product)
            );
            case Condition.And(var operands) -> mergeRelationIndices(
                operands.stream().map(operand -> findSingleRelationIndex(operand, product)).toList()
            );
            case Condition.Or(var operands) -> mergeRelationIndices(
                operands.stream().map(operand -> findSingleRelationIndex(operand, product)).toList()
            );
            case Condition.Not(var operand) -> findSingleRelationIndex(operand, product);
            case Condition.Exists _ -> Optional.empty();
            case Condition.InSubquery(var left, _, _) -> findSingleRelationIndex(left, product);
        };
    }

    private static Optional<Integer> findSingleRelationIndex(IRExpression expression, Product product) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) -> findRelation(columnName, product).map(RelInfo::relIndex);
            case IRExpression.Literal _ -> Optional.of(-1);
            case IRExpression.BinaryOp(var left, _, var right) -> mergeRelationIndices(
                findSingleRelationIndex(left, product),
                findSingleRelationIndex(right, product)
            );
            case IRExpression.Cast(var expr, _) -> findSingleRelationIndex(expr, product);
            case IRExpression.FunctionCall(_, var arguments) -> mergeRelationIndices(
                arguments.stream().map(argument -> findSingleRelationIndex(argument, product)).toList()
            );
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var relationIndices = new ArrayList<Optional<Integer>>();
                whens.forEach(when -> {
                    relationIndices.add(findSingleRelationIndex(when.condition(), product));
                    relationIndices.add(findSingleRelationIndex(when.result(), product));
                });
                elseExpr.stream().forEach(expr -> relationIndices.add(findSingleRelationIndex(expr, product)));
                yield mergeRelationIndices(relationIndices);
            }
            case IRExpression.ScalarSubquery _, IRExpression.Aggregate _ -> Optional.empty();
        };
    }

    private static Optional<Integer> mergeRelationIndices(Optional<Integer> left, Optional<Integer> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        if (left.get() == -1) {
            return right;
        }
        if (right.get() == -1) {
            return left;
        }
        return left.equals(right) ? left : Optional.empty();
    }

    private static Optional<Integer> mergeRelationIndices(List<Optional<Integer>> relationIndices) {
        Optional<Integer> current = Optional.of(-1);
        for (var relationIndex : relationIndices) {
            current = mergeRelationIndices(current, relationIndex);
            if (current.isEmpty()) {
                return Optional.empty();
            }
        }
        return current.filter(index -> index != -1);
    }

    private record RelInfo(int relIndex, String rawAttr) {
    }

    private static Optional<RelInfo> findRelation(String qualifiedAttr, Product product) {
        var relations = product.relations();
        for (int i = 0; i < relations.size(); i++) {
            var rel = relations.get(i);
            String prefix = rel.alias() + "_";
            if (qualifiedAttr.startsWith(prefix)) {
                String rawAttr = qualifiedAttr.substring(prefix.length());
                if (rel.attributes().contains(rawAttr)) {
                    return Optional.of(new RelInfo(i, rawAttr));
                }
            }
        }
        return Optional.empty();
    }
}
