package nnsql.query.optim;

import nnsql.query.ir.Condition;
import nnsql.query.ir.IRExpression;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static nnsql.util.Option.None;
import static nnsql.util.Option.Some;

final class PredicatePlacementAnalyzer {
    private final ProductScope scope;
    private final PredicateDependencyAnalyzer dependencyAnalyzer;
    private final boolean allowLocalPredicatePushdown;
    private final Set<String> reservedRelationAliases;

    PredicatePlacementAnalyzer(
        ProductScope scope,
        boolean allowLocalPredicatePushdown,
        Set<String> reservedRelationAliases
    ) {
        this.scope = scope;
        this.dependencyAnalyzer = new PredicateDependencyAnalyzer(scope);
        this.allowLocalPredicatePushdown = allowLocalPredicatePushdown;
        this.reservedRelationAliases = Set.copyOf(reservedRelationAliases);
    }

    PredicatePlacement analyze(Condition condition) {
        return switch (condition) {
            case Condition.And(var operands) -> analyzeConjunction(operands);
            case Condition.Or(var branches) -> analyzeDisjunction(branches);
            default -> analyzeAtomic(condition);
        };
    }

    private PredicatePlacement analyzeConjunction(List<Condition> operands) {
        var placement = PredicatePlacement.empty();
        for (var operand : operands) {
            placement = placement.merge(analyzeConjunct(operand));
        }
        return placement;
    }

    private PredicatePlacement analyzeConjunct(Condition condition) {
        return switch (condition) {
            case Condition.Or(var branches) -> analyzeDisjunction(branches);
            default -> analyzeAtomic(condition);
        };
    }

    private PredicatePlacement analyzeDisjunction(List<Condition> branches) {
        var branchOperands = branches.stream()
            .map(Conditions::conjunctionOperands)
            .toList();
        var commonJoins = commonJoinPredicates(branchOperands);
        var reducedBranchOperands = removeCommonJoins(branchOperands, commonJoins);
        var residual = Condition.or(reducedBranchOperands.stream()
            .map(Conditions::conjunction)
            .toList());

        return joinPlacement(commonJoins)
            .merge(localPlacement(reducedBranchOperands))
            .merge(PredicatePlacement.residual(residual));
    }

    private PredicatePlacement analyzeAtomic(Condition condition) {
        return switch (tryJoinPredicate(condition)) {
            case Some(var joinPredicate) -> PredicatePlacement.join(joinPredicate);
            case None<JoinPredicate> _ -> analyzeLocalOrResidual(condition);
        };
    }

    private PredicatePlacement analyzeLocalOrResidual(Condition condition) {
        if (!allowLocalPredicatePushdown) {
            return PredicatePlacement.residual(condition);
        }

        return switch (dependencyAnalyzer.find(condition).localRelation()) {
            case Some(var relation) when canPushInto(relation) -> PredicatePlacement.local(relation, condition);
            case Some<ProductRelation> _, None<ProductRelation> _ -> PredicatePlacement.residual(condition);
        };
    }

    private PredicatePlacement joinPlacement(List<Condition> joinConditions) {
        var placement = PredicatePlacement.empty();
        for (var condition : joinConditions) {
            placement = switch (tryJoinPredicate(condition)) {
                case Some(var joinPredicate) -> placement.merge(PredicatePlacement.join(joinPredicate));
                case None<JoinPredicate> _ -> placement;
            };
        }
        return placement;
    }

    private PredicatePlacement localPlacement(List<List<Condition>> branchOperands) {
        if (!allowLocalPredicatePushdown) {
            return PredicatePlacement.empty();
        }

        var localPredicates = new LinkedHashMap<ProductRelation, List<Condition>>();
        for (var relation : scope.relations()) {
            if (!canPushInto(relation)) {
                continue;
            }

            localConditionForEveryBranch(relation, branchOperands).stream()
                .forEach(condition -> localPredicates.put(relation, List.of(condition)));
        }

        return new PredicatePlacement(List.of(), localPredicates, Option.none());
    }

    private Option<Condition> localConditionForEveryBranch(
        ProductRelation relation,
        List<List<Condition>> branchOperands
    ) {
        var branchLocalConditions = new ArrayList<Condition>();
        for (var branch : branchOperands) {
            var localOperands = branch.stream()
                .filter(operand -> dependencyAnalyzer.find(operand).equals(RelationDependency.single(relation)))
                .toList();
            if (localOperands.isEmpty()) {
                return Option.none();
            }
            branchLocalConditions.add(Conditions.conjunction(localOperands));
        }

        return Option.some(branchLocalConditions.size() == 1
            ? branchLocalConditions.getFirst()
            : Condition.or(branchLocalConditions));
    }

    private List<Condition> commonJoinPredicates(List<List<Condition>> branchOperands) {
        if (branchOperands.isEmpty()) {
            return List.of();
        }

        var firstBranchJoins = branchOperands.getFirst().stream()
            .filter(condition -> switch (tryJoinPredicate(condition)) {
                case Some<JoinPredicate> _ -> true;
                case None<JoinPredicate> _ -> false;
            })
            .collect(Collectors.toSet());

        return firstBranchJoins.stream()
            .filter(joinPredicate -> branchOperands.stream().skip(1)
                .allMatch(branch -> branch.contains(joinPredicate)))
            .toList();
    }


    private List<List<Condition>> removeCommonJoins(
        List<List<Condition>> branchOperands,
        List<Condition> commonJoins
    ) {
        var commonJoinSet = Set.copyOf(commonJoins);
        return branchOperands.stream()
            .map(operands -> operands.stream()
                .filter(condition -> !commonJoinSet.contains(condition))
                .toList())
            .toList();
    }

    private Option<JoinPredicate> tryJoinPredicate(Condition condition) {
        return switch (condition) {
            case Condition.Comparison(
                IRExpression.ColumnRef(var leftColumn),
                IRExpression.ColumnRef(var rightColumn),
                var operator
            ) when "=".equals(operator) -> joinPredicate(scope.resolve(leftColumn), scope.resolve(rightColumn));
            default -> Option.none();
        };
    }

    private Option<JoinPredicate> joinPredicate(
        Option<ProductScope.Attribute> leftAttribute,
        Option<ProductScope.Attribute> rightAttribute
    ) {
        return switch (leftAttribute) {
            case None<ProductScope.Attribute> _ -> Option.none();
            case Some(var left) -> joinPredicate(left, rightAttribute);
        };
    }

    private Option<JoinPredicate> joinPredicate(
        ProductScope.Attribute left,
        Option<ProductScope.Attribute> rightAttribute
    ) {
        return switch (rightAttribute) {
            case None<ProductScope.Attribute> _ -> Option.none();
            case Some(var right) when left.relation().position() == right.relation().position() -> Option.none();
            case Some(var right) -> Option.some(new JoinPredicate(
                left.relation().position(), left.name(),
                right.relation().position(), right.name()
            ));
        };
    }

    private boolean canPushInto(ProductRelation relation) {
        return relation.isTable() && !reservedRelationAliases.contains(relation.alias());
    }
}
