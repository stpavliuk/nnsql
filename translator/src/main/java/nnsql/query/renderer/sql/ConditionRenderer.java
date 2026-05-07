package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.Condition;
import nnsql.query.ir.Filter;
import nnsql.query.ir.IRExpression;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.renderer.RenderContext;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;

import static nnsql.query.renderer.sql.Sql.*;

record ConditionRenderer(ComparisonRenderer comparisonRenderer, SqlDialect dialect) {

    ConditionRenderer(BiFunction<IRNode, RenderContext, String> subqueryRenderer, SqlDialect dialect) {
        this(new ComparisonRenderer(subqueryRenderer, dialect), dialect);
    }

    java.util.Optional<OptimizedFilterIdSelect> renderOptimizedFilterIdSelect(
        Condition condition,
        String relationName,
        RenderContext ctx
    ) {
        var fullyInline = inlineCondition(condition, relationName);
        if (fullyInline.isPresent()) {
            return java.util.Optional.of(buildInlineFilterIdSelect(
                relationName,
                List.of(fullyInline.get()),
                List.of(),
                List.of(),
                List.of()
            ));
        }

        if (!(condition instanceof Condition.And(var operands))) {
            return java.util.Optional.empty();
        }

        var inlinePredicates = new ArrayList<InlinePredicate>();
        var inlinedCorrelatedComparisons = new ArrayList<ComparisonRenderer.InlinedCorrelatedComparison>();
        var inlinedCorrelatedExists = new ArrayList<InlinedCorrelatedExists>();
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
            if (inlineCorrelatedExists(operand, relationName, ctx)
                .map(inlinedCorrelatedExists::add)
                .orElse(false)) {
                continue;
            }
            fallbackConditions.add(paren(render(operand, relationName, false, ctx)));
        }

        if (inlinePredicates.isEmpty()
            && inlinedCorrelatedComparisons.isEmpty()
            && inlinedCorrelatedExists.isEmpty()) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(buildInlineFilterIdSelect(
            relationName,
            inlinePredicates,
            inlinedCorrelatedComparisons,
            inlinedCorrelatedExists,
            fallbackConditions
        ));
    }

    private OptimizedFilterIdSelect buildInlineFilterIdSelect(
        String relationName,
        List<InlinePredicate> inlinePredicates,
        List<ComparisonRenderer.InlinedCorrelatedComparison> inlinedCorrelatedComparisons,
        List<InlinedCorrelatedExists> inlinedCorrelatedExists,
        List<Expression> fallbackConditions
    ) {
        var projectedAttributes = inlinePredicates.stream()
            .flatMap(inline -> inline.requiredColumns().stream())
            .distinct()
            .toList();
        var requiredColumns = new ArrayList<String>();
        requiredColumns.addAll(projectedAttributes);
        requiredColumns.addAll(inlinedCorrelatedComparisons.stream()
            .flatMap(inlined -> inlined.requiredColumns().stream())
            .toList());
        requiredColumns.addAll(inlinedCorrelatedExists.stream()
            .flatMap(inlined -> inlined.requiredColumns().stream())
            .toList());
        requiredColumns = new ArrayList<>(requiredColumns.stream().distinct().toList());

        var ps = new PlainSelect();

        var joins = new ArrayList<net.sf.jsqlparser.statement.select.Join>();
        var whereConditions = new ArrayList<Expression>();
        var attributeTables = new LinkedHashMap<String, net.sf.jsqlparser.schema.Table>();
        if (requiredColumns.isEmpty() || !fallbackConditions.isEmpty()) {
            var idTbl = table(idTable(relationName));
            ps.addSelectItem(column(idTbl, "id"));
            ps.setFromItem(idTbl);
            addFilterAttributeJoins(relationName, requiredColumns, column(idTbl, "id"), joins, attributeTables);
        } else {
            var anchorColumn = requiredColumns.removeFirst();
            var anchorTable = table(attrTable(relationName, anchorColumn));
            ps.addSelectItem(column(anchorTable, "id"));
            ps.setFromItem(anchorTable);
            attributeTables.put(anchorColumn, anchorTable);
            addFilterAttributeJoins(relationName, requiredColumns, column(anchorTable, "id"), joins, attributeTables);
        }

        projectedAttributes.forEach(attribute -> {
            var attrTable = attributeTables.get(attribute);
            if (attrTable != null) {
                ps.addSelectItem(column(attrTable, "v"), new net.sf.jsqlparser.expression.Alias(
                    projectedAttributeColumn(attribute),
                    true
                ));
            }
        });

        inlinePredicates.stream()
            .map(InlinePredicate::predicate)
            .map(Sql::paren)
            .forEach(whereConditions::add);
        for (var inlinedComparison : inlinedCorrelatedComparisons) {
            whereConditions.addAll(inlinedComparison.predicates());
        }
        inlinedCorrelatedExists.stream()
            .map(InlinedCorrelatedExists::predicate)
            .forEach(whereConditions::add);
        whereConditions.addAll(fallbackConditions);

        for (var inlinedComparison : inlinedCorrelatedComparisons) {
            inlinedComparison.fromItems().forEach(fromItem -> joins.add(simpleJoin(fromItem)));
        }
        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }
        ps.setWhere(
            whereConditions.isEmpty()
                ? new net.sf.jsqlparser.expression.BooleanValue(true)
                : andAll(whereConditions)
        );

        return new OptimizedFilterIdSelect(ps, projectedAttributes);
    }

    private static void addFilterAttributeJoins(
        String relationName,
        List<String> requiredColumns,
        Expression anchorIdExpr,
        List<net.sf.jsqlparser.statement.select.Join> joins,
        LinkedHashMap<String, net.sf.jsqlparser.schema.Table> attributeTables
    ) {
        for (var columnName : requiredColumns) {
            var attrTbl = table(attrTable(relationName, columnName));
            joins.add(join(
                attrTbl,
                new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                    column(attrTbl, "id"),
                    anchorIdExpr
                )
            ));
            attributeTables.put(columnName, attrTbl);
        }
    }

    static String projectedAttributeColumn(String attribute) {
        return "filter_attr_" + attribute;
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
                renderExists(exists, negate, relationName, ctx);

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

    private Expression renderExists(Condition.Exists existsCondition, boolean negate,
                                     String relationName, RenderContext ctx) {
        var effectiveNegate = negate != existsCondition.isNegated();

        if (existsCondition.correlations().isEmpty()) {
            var subquery = comparisonRenderer.renderExistsSubquery(existsCondition.subquery(), ctx);
            return effectiveNegate ? notExists(subquery) : exists(subquery);
        }

        var subquery = renderCorrelatedExists(
            existsCondition.subquery(), existsCondition.correlations(), relationName, ctx, false);
        return effectiveNegate ? notExists(subquery) : exists(subquery);
    }

    private java.util.Optional<InlinedCorrelatedExists> inlineCorrelatedExists(
        Condition condition,
        String outerRelationName,
        RenderContext ctx
    ) {
        return switch (condition) {
            case Condition.Exists existsCondition ->
                inlineCorrelatedExists(existsCondition, outerRelationName, ctx, false);
            case Condition.Not(var operand) when operand instanceof Condition.Exists existsCondition ->
                inlineCorrelatedExists(existsCondition, outerRelationName, ctx, true);
            default -> java.util.Optional.empty();
        };
    }

    private java.util.Optional<InlinedCorrelatedExists> inlineCorrelatedExists(
        Condition.Exists existsCondition,
        String outerRelationName,
        RenderContext ctx,
        boolean negatedByWrapper
    ) {
        if (existsCondition.correlations().isEmpty()) {
            return java.util.Optional.empty();
        }

        var subquery = renderCorrelatedExists(
            existsCondition.subquery(),
            existsCondition.correlations(),
            outerRelationName,
            ctx,
            true
        );
        var useNotExists = existsCondition.isNegated() != negatedByWrapper;
        var predicate = useNotExists ? notExists(subquery) : exists(subquery);
        var requiredColumns = existsCondition.correlations().stream()
            .map(IRExpression.Correlation::outerAttribute)
            .distinct()
            .toList();
        return java.util.Optional.of(new InlinedCorrelatedExists(predicate, requiredColumns));
    }

    private PlainSelect renderCorrelatedExists(
        IRNode subqueryIR,
        List<IRExpression.Correlation> correlations,
        String outerRelationName,
        RenderContext ctx,
        boolean useOuterJoinedAttributes
    ) {
        var directSubquery = renderDirectCorrelatedExists(
            subqueryIR,
            correlations,
            outerRelationName,
            ctx,
            useOuterJoinedAttributes
        );
        if (directSubquery.isSome()) {
            return directSubquery.get();
        }

        var coreIR = stripProjection(subqueryIR);
        var innerBaseName = comparisonRenderer.renderSubqueryBaseName(coreIR, ctx);

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());

        var joins = new ArrayList<net.sf.jsqlparser.statement.select.Join>();
        var conditions = new ArrayList<Expression>();
        var firstCorrelation = correlations.getFirst();
        var anchorInnerAttrTbl = table(attrTable(innerBaseName, firstCorrelation.innerAttribute()));
        ps.setFromItem(anchorInnerAttrTbl);

        var innerAttrTables = new LinkedHashMap<String, net.sf.jsqlparser.schema.Table>();
        innerAttrTables.put(firstCorrelation.innerAttribute(), anchorInnerAttrTbl);

        for (var correlation : correlations) {
            var innerAttrTbl = innerAttrTables.computeIfAbsent(correlation.innerAttribute(), innerAttribute -> {
                var table = table(attrTable(innerBaseName, innerAttribute));
                joins.add(join(
                    table,
                    new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                        column(table, "id"),
                        column(anchorInnerAttrTbl, "id")
                    )
                ));
                return table;
            });

            conditions.add(comparison(
                correlatedOuterValueExpr(
                    outerRelationName,
                    correlation.outerAttribute(),
                    joins,
                    useOuterJoinedAttributes
                ),
                correlation.operator(),
                column(innerAttrTbl, "v")
            ));
        }

        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }
        ps.setWhere(andAll(conditions));
        return ps;
    }

    private Option<PlainSelect> renderDirectCorrelatedExists(
        IRNode subqueryIR,
        List<IRExpression.Correlation> correlations,
        String outerRelationName,
        RenderContext ctx,
        boolean useOuterJoinedAttributes
    ) {
        return simpleSingleTableSubquery(stripProjection(subqueryIR))
            .flatMap(subquery -> buildDirectCorrelatedExists(
                subquery,
                correlations,
                outerRelationName,
                ctx,
                useOuterJoinedAttributes
            ));
    }

    private Option<SimpleCorrelatedExistsSubquery> simpleSingleTableSubquery(IRNode node) {
        return switch (node) {
            case Product product -> simpleSingleTableProduct(product)
                .map(relation -> new SimpleCorrelatedExistsSubquery(relation, Option.none()));
            case Filter(var input, var condition, _) -> simpleSingleTableSubquery(input)
                .map(subquery -> new SimpleCorrelatedExistsSubquery(
                    subquery.relation(),
                    Option.some(condition)
                ));
            default -> Option.none();
        };
    }

    private Option<Relation.Table> simpleSingleTableProduct(Product product) {
        if (product.relations().size() != 1 || !product.joinPredicates().isEmpty()) {
            return Option.none();
        }

        return switch (product.relations().getFirst()) {
            case Relation.Table relation -> Option.some(relation);
            case Relation.Subquery _ -> Option.none();
        };
    }

    private Option<PlainSelect> buildDirectCorrelatedExists(
        SimpleCorrelatedExistsSubquery subquery,
        List<IRExpression.Correlation> correlations,
        String outerRelationName,
        RenderContext ctx,
        boolean useOuterJoinedAttributes
    ) {
        var innerAttributes = new ArrayList<String>();
        correlations.stream()
            .map(IRExpression.Correlation::innerAttribute)
            .forEach(innerAttributes::add);
        subquery.localCondition()
            .map(ExpressionSqlRenderer::collectColumnsFromCondition)
            .stream()
            .flatMap(List::stream)
            .forEach(innerAttributes::add);
        innerAttributes = new ArrayList<>(innerAttributes.stream().distinct().toList());
        if (innerAttributes.isEmpty()
            || !allAttributesBelongToRelation(innerAttributes, subquery.relation())) {
            return Option.none();
        }

        var attrAliases = new LinkedHashMap<String, net.sf.jsqlparser.schema.Table>();
        for (var attribute : innerAttributes) {
            var attrTable = table(attrTable(
                subquery.relation().tableName(),
                unqualifiedAttribute(attribute, subquery.relation())
            ));
            attrAliases.put(attribute, attrTable);
        }

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());

        var anchorAttribute = innerAttributes.getFirst();
        var anchorTable = attrAliases.get(anchorAttribute);
        ps.setFromItem(anchorTable);

        var joins = new ArrayList<net.sf.jsqlparser.statement.select.Join>();
        attrAliases.forEach((attribute, attrTable) -> {
            if (attribute.equals(anchorAttribute)) {
                return;
            }
            joins.add(join(
                attrTable,
                new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                    column(attrTable, "id"),
                    column(anchorTable, "id")
                )
            ));
        });

        var conditions = new ArrayList<Expression>();
        for (var correlation : correlations) {
            conditions.add(comparison(
                correlatedOuterValueExpr(
                    outerRelationName,
                    correlation.outerAttribute(),
                    joins,
                    useOuterJoinedAttributes
                ),
                correlation.operator(),
                column(attrAliases.get(correlation.innerAttribute()), "v")
            ));
        }

        if (subquery.localCondition().isSome()) {
            var localCondition = renderDirectInnerCondition(subquery.localCondition().get(), attrAliases);
            if (localCondition.isNone()) {
                return Option.none();
            }
            conditions.add(localCondition.get());
        }

        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }
        ps.setWhere(andAll(conditions));
        return Option.some(ps);
    }

    private boolean allAttributesBelongToRelation(List<String> attributes, Relation.Table relation) {
        var sourceAttributes = new java.util.HashSet<>(relation.attributes());
        return attributes.stream()
            .map(attribute -> unqualifiedAttribute(attribute, relation))
            .allMatch(sourceAttributes::contains);
    }

    private Option<Expression> renderDirectInnerCondition(
        Condition condition,
        LinkedHashMap<String, net.sf.jsqlparser.schema.Table> attrAliases
    ) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var operator) ->
                renderDirectInnerExpression(left, attrAliases)
                    .flatMap(leftExpr -> renderDirectInnerExpression(right, attrAliases)
                        .map(rightExpr -> comparison(leftExpr, operator, rightExpr)));
            case Condition.Like(var left, var pattern, var negated) ->
                renderDirectInnerExpression(left, attrAliases)
                    .flatMap(leftExpr -> renderDirectInnerExpression(pattern, attrAliases)
                        .map(patternExpr -> like(leftExpr, patternExpr, negated)));
            case Condition.And(var operands) -> renderDirectInnerLogicalCondition(operands, attrAliases, true);
            case Condition.Or(var operands) -> renderDirectInnerLogicalCondition(operands, attrAliases, false);
            case Condition.Not(var operand) -> renderDirectInnerCondition(operand, attrAliases)
                .map(expr -> not(paren(expr)));
            case Condition.IsNull _, Condition.Exists _, Condition.InSubquery _ -> Option.none();
        };
    }

    private Option<Expression> renderDirectInnerLogicalCondition(
        List<Condition> operands,
        LinkedHashMap<String, net.sf.jsqlparser.schema.Table> attrAliases,
        boolean conjunction
    ) {
        var rendered = new ArrayList<Expression>();
        for (var operand : operands) {
            var expression = renderDirectInnerCondition(operand, attrAliases);
            if (expression.isNone()) {
                return Option.none();
            }
            rendered.add(paren(expression.get()));
        }

        return Option.some(conjunction ? andAll(rendered) : orAll(rendered));
    }

    private Option<Expression> renderDirectInnerExpression(
        IRExpression expression,
        LinkedHashMap<String, net.sf.jsqlparser.schema.Table> attrAliases
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) -> Option.ofNullable(attrAliases.get(columnName))
                .map(attrTable -> column(attrTable, "v"));
            case IRExpression.Literal literal -> Option.some(literal(literal));
            case IRExpression.BinaryOp(var left, var operator, var right) ->
                renderDirectInnerExpression(left, attrAliases)
                    .flatMap(leftExpr -> renderDirectInnerExpression(right, attrAliases)
                        .map(rightExpr -> arithmetic(leftExpr, operator.toSql(), rightExpr)));
            case IRExpression.Cast(var inner, var targetType) ->
                renderDirectInnerExpression(inner, attrAliases)
                    .map(innerExpr -> new net.sf.jsqlparser.expression.CastExpression("CAST", innerExpr, targetType));
            case IRExpression.FunctionCall _, IRExpression.CaseWhen _,
                 IRExpression.Aggregate _, IRExpression.ScalarSubquery _ -> Option.none();
        };
    }

    private String unqualifiedAttribute(String qualifiedAttribute, Relation.Table relation) {
        var prefix = relation.alias() + "_";
        return qualifiedAttribute.startsWith(prefix)
            ? qualifiedAttribute.substring(prefix.length())
            : qualifiedAttribute;
    }

    private Expression correlatedOuterValueExpr(
        String outerRelationName,
        String outerAttribute,
        List<net.sf.jsqlparser.statement.select.Join> joins,
        boolean useOuterJoinedAttributes
    ) {
        var outerAttrTbl = table(attrTable(outerRelationName, outerAttribute));
        if (useOuterJoinedAttributes) {
            return column(outerAttrTbl, "v");
        }

        var outerIdTbl = table(idTable(outerRelationName));
        joins.add(join(
            outerAttrTbl,
            new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                column(outerAttrTbl, "id"),
                column(outerIdTbl, "id")
            )
        ));
        return column(outerAttrTbl, "v");
    }

    private static IRNode stripProjection(IRNode node) {
        return switch (node) {
            case nnsql.query.ir.Return r -> r.input();
            case nnsql.query.ir.DuplElim d -> stripProjection(d.input());
            case nnsql.query.ir.Sort s -> stripProjection(s.input());
            default -> node;
        };
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
        var fullyInline = inlineCondition(Condition.and(operands), relationName);
        if (fullyInline.isPresent()) {
            return renderInlinePredicateExists(List.of(fullyInline.get()), relationName);
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
                .map(inline -> paren(render(inline.sourceCondition(), relationName, false, ctx)))
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
            return andAll(inlinePredicates.stream().map(InlinePredicate::predicate).map(Sql::paren).toList());
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

        whereConditions.addAll(inlinePredicates.stream().map(InlinePredicate::predicate).map(Sql::paren).toList());
        ps.setWhere(andAll(whereConditions));

        return exists(ps);
    }

    private java.util.Optional<InlinePredicate> inlineCondition(Condition condition, String relationName) {
        return switch (condition) {
            case Condition.Comparison comparison -> inlineComparison(
                comparison,
                comparison.left(),
                comparison.operator(),
                comparison.right(),
                relationName
            );
            case Condition.Like like -> inlineComparison(
                like,
                like.left(),
                like.isNegated() ? "NOT LIKE" : "LIKE",
                like.pattern(),
                relationName
            );
            case Condition.And(var operands) ->
                inlineLogicalCondition(Condition.and(operands), operands, relationName, true);
            case Condition.Or(var operands) ->
                inlineLogicalCondition(Condition.or(operands), operands, relationName, false);
            case Condition.Not(var operand) -> inlineCondition(operand, relationName)
                .map(inline -> new InlinePredicate(
                    condition,
                    not(paren(inline.predicate())),
                    inline.requiredColumns()
                ));
            default -> java.util.Optional.empty();
        };
    }

    private java.util.Optional<InlinePredicate> inlineLogicalCondition(
        Condition sourceCondition,
        List<Condition> operands,
        String relationName,
        boolean conjunction
    ) {
        var inlinedOperands = operands.stream()
            .map(operand -> inlineCondition(operand, relationName))
            .toList();
        if (inlinedOperands.stream().anyMatch(java.util.Optional::isEmpty)) {
            return java.util.Optional.empty();
        }

        var predicates = inlinedOperands.stream()
            .map(java.util.Optional::orElseThrow)
            .toList();

        if (!conjunction && !haveMatchingRequiredColumns(predicates)) {
            return java.util.Optional.empty();
        }

        var requiredColumns = conjunction
            ? predicates.stream()
                .flatMap(inline -> inline.requiredColumns().stream())
                .distinct()
                .toList()
            : predicates.getFirst().requiredColumns().stream().distinct().toList();
        var predicateExpressions = predicates.stream()
            .map(InlinePredicate::predicate)
            .toList();
        var groupedPredicates = predicateExpressions.size() > 1
            ? predicateExpressions.stream().map(Sql::paren).toList()
            : predicateExpressions;
        var combinedPredicate = switch (predicateExpressions.size()) {
            case 0 -> new net.sf.jsqlparser.expression.BooleanValue(conjunction);
            case 1 -> predicateExpressions.getFirst();
            default -> conjunction ? andAll(groupedPredicates) : orAll(groupedPredicates);
        };

        return java.util.Optional.of(new InlinePredicate(sourceCondition, combinedPredicate, requiredColumns));
    }

    private boolean haveMatchingRequiredColumns(List<InlinePredicate> predicates) {
        if (predicates.isEmpty()) {
            return true;
        }

        var firstColumns = new java.util.LinkedHashSet<>(predicates.getFirst().requiredColumns());
        return predicates.stream()
            .skip(1)
            .allMatch(predicate -> firstColumns.equals(new java.util.LinkedHashSet<>(predicate.requiredColumns())));
    }

    private java.util.Optional<InlinePredicate> inlineComparison(
        Condition sourceCondition,
        IRExpression left,
        String operator,
        IRExpression right,
        String relationName
    ) {
        if (!isInlineExpression(left) || !isInlineExpression(right)) {
            return java.util.Optional.empty();
        }

        var predicate = comparison(
            ExpressionSqlRenderer.toSqlExpr(left, relationName, dialect),
            operator,
            ExpressionSqlRenderer.toSqlExpr(right, relationName, dialect)
        );
        var requiredColumns = ExpressionSqlRenderer.collectColumns(left, right);
        return java.util.Optional.of(new InlinePredicate(sourceCondition, predicate, requiredColumns));
    }

    private boolean isInlineExpression(IRExpression expression) {
        return switch (expression) {
            case IRExpression.ColumnRef _, IRExpression.Literal _ -> true;
            case IRExpression.FunctionCall(_, var arguments) ->
                arguments.stream().allMatch(this::isInlineExpression);
            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.Aggregate _, IRExpression.ScalarSubquery _ -> false;
        };
    }

    private record InlinePredicate(Condition sourceCondition, Expression predicate, List<String> requiredColumns) {
    }

    record OptimizedFilterIdSelect(PlainSelect select, List<String> projectedAttributes) {
    }

    private record InlinedCorrelatedExists(Expression predicate, List<String> requiredColumns) {
    }

    private record SimpleCorrelatedExistsSubquery(
        Relation.Table relation,
        Option<Condition> localCondition
    ) {
    }

    private Expression renderLogical(List<Condition> operands, boolean negate,
                                      String relationName, RenderContext ctx, boolean useAnd) {
        var rendered = operands.stream()
            .map(cond -> paren(render(cond, relationName, negate, ctx)))
            .toList();

        return useAnd ? andAll(rendered) : orAll(rendered);
    }
}
