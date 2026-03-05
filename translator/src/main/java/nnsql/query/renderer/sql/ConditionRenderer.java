package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.Condition;
import nnsql.query.ir.IRExpression;
import nnsql.query.ir.IRNode;
import nnsql.query.renderer.RenderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static nnsql.query.renderer.sql.Sql.*;

record ConditionRenderer(ComparisonRenderer comparisonRenderer) {

    ConditionRenderer(BiFunction<IRNode, RenderContext, String> subqueryRenderer) {
        this(new ComparisonRenderer(subqueryRenderer));
    }

    java.util.Optional<PlainSelect> renderOptimizedFilterIdSelect(
        Condition condition,
        String relationName,
        RenderContext ctx
    ) {
        if (!(condition instanceof Condition.And(var operands)) || operands.size() < 4) {
            return java.util.Optional.empty();
        }

        var inlinePredicates = new ArrayList<InlinePredicate>();
        var inlinedCorrelatedComparisons = new ArrayList<ComparisonRenderer.InlinedCorrelatedComparison>();
        var fallbackConditions = new ArrayList<Expression>();

        for (var operand : operands) {
            if (inlineCondition(operand, relationName).map(inlinePredicates::add).orElse(false)) {
                continue;
            }
            if (operand instanceof Condition.Comparison comparison
                && comparisonRenderer.inlineCorrelatedComparison(comparison, relationName, ctx)
                .map(inlinedCorrelatedComparisons::add)
                .orElse(false)) {
                continue;
            }
            fallbackConditions.add(render(operand, relationName, false, ctx));
        }

        if (inlinePredicates.size() < 3 && inlinedCorrelatedComparisons.isEmpty()) {
            return java.util.Optional.empty();
        }

        var requiredColumns = new ArrayList<String>();
        requiredColumns.addAll(inlinePredicates.stream()
            .flatMap(inline -> inline.requiredColumns().stream())
            .toList());
        requiredColumns.addAll(inlinedCorrelatedComparisons.stream()
            .flatMap(inlined -> inlined.requiredColumns().stream())
            .toList());
        requiredColumns = new ArrayList<>(requiredColumns.stream().distinct().toList());

        var idTbl = table(idTable(relationName));
        var ps = new PlainSelect();
        ps.addSelectItem(column(idTbl, "id"));
        ps.setFromItem(idTbl);

        var joins = new ArrayList<net.sf.jsqlparser.statement.select.Join>();
        var whereConditions = new ArrayList<Expression>();
        if (!requiredColumns.isEmpty()) {
            addComputedExprAttributeJoins(
                relationName,
                requiredColumns,
                column(idTbl, "id"),
                false,
                Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
                joins,
                whereConditions
            );
        }

        whereConditions.addAll(inlinePredicates.stream().map(InlinePredicate::predicate).toList());
        for (var inlinedComparison : inlinedCorrelatedComparisons) {
            joins.add(simpleJoin(inlinedComparison.fromItem()));
            whereConditions.addAll(inlinedComparison.predicates());
        }
        whereConditions.addAll(fallbackConditions);

        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }
        ps.setWhere(
            whereConditions.isEmpty()
                ? new net.sf.jsqlparser.expression.BooleanValue(true)
                : andAll(whereConditions)
        );

        return java.util.Optional.of(ps);
    }

    Expression renderTrue(Condition condition, String relationName, RenderContext ctx) {
        return render(condition, relationName, false, ctx);
    }

    Expression renderFalse(Condition condition, String relationName, RenderContext ctx) {
        return render(condition, relationName, true, ctx);
    }

    private Expression render(Condition condition, String relationName, boolean negate, RenderContext ctx) {
        return switch (condition) {
            case Condition.Comparison comp ->
                negate ? comparisonRenderer.renderFalse(comp, relationName, ctx)
                       : comparisonRenderer.renderTrue(comp, relationName, ctx);

            case Condition.IsNull(var attr, var negated) ->
                renderIsNull(attr, negated == negate, relationName);

            case Condition.Like like ->
                renderLike(like, negate, relationName, ctx);

            case Condition.Exists exists ->
                renderExists(exists, negate, ctx);

            case Condition.InSubquery inSubquery ->
                comparisonRenderer.renderInSubquery(inSubquery, relationName, negate, ctx);

            case Condition.And and ->
                negate
                    ? renderLogical(and.operands(), true, relationName, ctx, false)
                    : renderConjunctiveAnd(and.operands(), relationName, ctx);

            case Condition.Or or ->
                renderLogical(or.operands(), negate, relationName, ctx, negate);

            case Condition.Not not ->
                render(not.operand(), relationName, !negate, ctx);
        };
    }

    private Expression renderLike(Condition.Like like, boolean negate, String relationName, RenderContext ctx) {
        var effectiveNegate = negate != like.isNegated();
        var operator = effectiveNegate ? "NOT LIKE" : "LIKE";
        var comparison = Condition.compare(like.left(), operator, like.pattern());
        return comparisonRenderer.renderTrue(comparison, relationName, ctx);
    }

    private Expression renderExists(Condition.Exists existsCondition, boolean negate, RenderContext ctx) {
        var effectiveNegate = negate != existsCondition.isNegated();
        var subquery = comparisonRenderer.renderExistsSubquery(existsCondition.subquery(), ctx);
        return effectiveNegate ? notExists(subquery) : exists(subquery);
    }

    private Expression renderIsNull(String attr, boolean isNull, String relationName) {
        var attrTbl = table(attrTable(relationName, attr));
        var idTbl = table(idTable(relationName));

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(attrTbl);
        ps.setWhere(new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
            column(attrTbl, "id"), column(idTbl, "id")
        ));

        return isNull ? notExists(ps) : exists(ps);
    }

    private Expression renderConjunctiveAnd(
        List<Condition> operands,
        String relationName,
        RenderContext ctx
    ) {
        if (operands.size() < 4) {
            return renderLogical(operands, false, relationName, ctx, true);
        }

        var inlinePredicates = new ArrayList<InlinePredicate>();
        var fallbackExpressions = new ArrayList<Expression>();

        for (var operand : operands) {
            inlineCondition(operand, relationName)
                .map(inlinePredicates::add)
                .orElseGet(() -> {
                    fallbackExpressions.add(paren(render(operand, relationName, false, ctx)));
                    return false;
                });
        }

        var conjunctionParts = new ArrayList<Expression>();
        if (inlinePredicates.size() >= 3) {
            conjunctionParts.add(paren(renderInlinePredicateExists(inlinePredicates, relationName)));
        } else {
            fallbackExpressions.addAll(inlinePredicates.stream()
                .map(inline -> paren(inline.predicate()))
                .toList());
        }
        conjunctionParts.addAll(fallbackExpressions);

        return switch (conjunctionParts.size()) {
            case 0 -> new net.sf.jsqlparser.expression.BooleanValue(true);
            case 1 -> conjunctionParts.getFirst();
            default -> andAll(conjunctionParts);
        };
    }

    private Expression renderInlinePredicateExists(
        List<InlinePredicate> inlinePredicates,
        String relationName
    ) {
        var requiredColumns = inlinePredicates.stream()
            .flatMap(inline -> inline.requiredColumns().stream())
            .distinct()
            .toList();
        if (requiredColumns.isEmpty()) {
            return andAll(inlinePredicates.stream().map(InlinePredicate::predicate).toList());
        }

        var idTbl = table(idTable(relationName));
        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(idTbl);

        var joins = new ArrayList<net.sf.jsqlparser.statement.select.Join>();
        var whereConditions = new ArrayList<Expression>();
        addComputedExprAttributeJoins(
            relationName,
            requiredColumns,
            column(idTbl, "id"),
            false,
            Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
            joins,
            whereConditions
        );
        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }

        whereConditions.addAll(inlinePredicates.stream().map(InlinePredicate::predicate).toList());
        ps.setWhere(andAll(whereConditions));

        return exists(ps);
    }

    private java.util.Optional<InlinePredicate> inlineCondition(Condition condition, String relationName) {
        return switch (condition) {
            case Condition.Comparison comparison -> inlineComparison(
                comparison.left(),
                comparison.operator(),
                comparison.right(),
                relationName
            );
            case Condition.Like like when !like.isNegated() -> inlineComparison(
                like.left(),
                "LIKE",
                like.pattern(),
                relationName
            );
            default -> java.util.Optional.empty();
        };
    }

    private java.util.Optional<InlinePredicate> inlineComparison(
        IRExpression left,
        String operator,
        IRExpression right,
        String relationName
    ) {
        if (!isInlineExpression(left) || !isInlineExpression(right)) {
            return java.util.Optional.empty();
        }

        var predicate = comparison(
            ExpressionSqlRenderer.toSqlExpr(left, relationName),
            operator,
            ExpressionSqlRenderer.toSqlExpr(right, relationName)
        );
        var requiredColumns = ExpressionSqlRenderer.collectColumns(left, right);
        return java.util.Optional.of(new InlinePredicate(predicate, requiredColumns));
    }

    private boolean isInlineExpression(IRExpression expression) {
        return switch (expression) {
            case IRExpression.ColumnRef _, IRExpression.Literal _ -> true;
            default -> false;
        };
    }

    private record InlinePredicate(Expression predicate, List<String> requiredColumns) {
    }

    private Expression renderLogical(List<Condition> operands, boolean negate,
                                      String relationName, RenderContext ctx, boolean useAnd) {
        var rendered = operands.stream()
            .map(cond -> paren(render(cond, relationName, negate, ctx)))
            .toList();

        return useAnd ? andAll(rendered) : orAll(rendered);
    }
}
