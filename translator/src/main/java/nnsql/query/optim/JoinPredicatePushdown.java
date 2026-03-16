package nnsql.query.optim;

import nnsql.query.ir.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JoinPredicatePushdown {

    public static IRNode optimize(IRNode node) {
        return switch (node) {
            case Product p -> optimizeProduct(p);
            case Filter f -> optimizeFilter(f);
            case Group g -> new Group(
                optimize(g.input()), g.groupingAttributes(),
                g.aggregates(), g.outputAttributes(), g.nodeId());
            case AggFilter af -> new AggFilter(
                optimize(af.input()), af.condition(), af.attributes());
            case Return r -> new Return(
                optimize(r.input()), r.selectedAttributes(), r.selectStar());
            case DuplElim d -> new DuplElim(optimize(d.input()), d.attributes());
            case Sort s -> new Sort(optimize(s.input()), s.keys(), s.limit());
        };
    }

    private static Product optimizeProduct(Product p) {
        var newRelations = p.relations().stream()
            .map(r -> switch (r) {
                case Relation.Subquery(var alias, var ir, var attrs) ->
                    Relation.subquery(alias, optimize(ir), attrs);
                case Relation.Table t -> (Relation) t;
            })
            .toList();
        return new Product(newRelations, p.nodeId(), p.joinPredicates());
    }

    private static IRNode optimizeFilter(Filter filter) {
        var optimizedInput = optimize(filter.input());

        if (!(optimizedInput instanceof Product product) || product.relations().size() < 2) {
            return new Filter(optimizedInput, filter.condition(), filter.attributes());
        }

        var normalized = factorOutCommonJoinPredicates(filter.condition(), product);
        var extraction = extractJoinPredicates(normalized, product);

        if (extraction.joinPredicates().isEmpty()) {
            return new Filter(optimizedInput, filter.condition(), filter.attributes());
        }

        var newProduct = new Product(
            product.relations(), product.nodeId(), extraction.joinPredicates());

        if (extraction.remainingCondition().isEmpty()) {
            return newProduct;
        }

        return new Filter(newProduct, extraction.remainingCondition().get(), filter.attributes());
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
