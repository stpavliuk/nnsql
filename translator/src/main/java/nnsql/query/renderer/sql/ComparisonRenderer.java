package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.*;
import nnsql.query.renderer.RenderContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static nnsql.query.renderer.sql.Sql.*;

record ComparisonRenderer(BiFunction<IRNode, RenderContext, String> subqueryRenderer, SqlDialect dialect) {

    record InlinedCorrelatedComparison(
        FromItem fromItem,
        List<Expression> predicates,
        List<String> requiredColumns
    ) {
    }

    record RenderedValueSubquery(
        PlainSelect values,
        PlainSelect nullRows
    ) {
    }

    String renderSubqueryBaseName(IRNode subquery, RenderContext ctx) {
        return subqueryRenderer.apply(subquery, ctx);
    }

    Expression renderTrue(Condition.Comparison comp, String rel, RenderContext ctx) {
        return render(comp, rel, false, ctx);
    }

    Expression renderFalse(Condition.Comparison comp, String rel, RenderContext ctx) {
        return render(comp, rel, true, ctx);
    }

    Expression renderInSubquery(
        Condition.InSubquery inSubquery,
        String rel,
        boolean negate,
        RenderContext ctx
    ) {
        var effectiveNegate = negate != inSubquery.isNegated();
        if (!effectiveNegate) {
            var directMembershipSubquery = renderDirectInMembershipSubquery(inSubquery.subquery());
            if (directMembershipSubquery.isPresent()) {
                return switch (inSubquery.left()) {
                    case IRExpression.ColumnRef(var col) ->
                        existsColumnInSubquery(rel, col, directMembershipSubquery.get(), false);
                    case IRExpression.Literal lit ->
                        inPredicate(literal(lit), directMembershipSubquery.get(), false);
                    case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                         IRExpression.FunctionCall _ ->
                        renderComputedInSubquery(inSubquery.left(), directMembershipSubquery.get(), rel, false);
                    case IRExpression.ScalarSubquery _ ->
                        throw unsupported("Scalar subquery on left side of IN");
                    case IRExpression.Aggregate _ ->
                        throw unsupported(inSubquery.left().getClass().getSimpleName());
                };
            }
        }

        var membershipSubquery = renderMembershipSubquery(
            inSubquery.subquery(),
            "IN subquery",
            ctx
        );

        return switch (inSubquery.left()) {
            case IRExpression.ColumnRef(var col) ->
                applyNotInNullGuard(
                    existsColumnInSubquery(rel, col, membershipSubquery.values(), effectiveNegate),
                    membershipSubquery.nullRows(),
                    effectiveNegate
                );

            case IRExpression.Literal lit ->
                applyNotInNullGuard(
                    inPredicate(literal(lit), membershipSubquery.values(), effectiveNegate),
                    membershipSubquery.nullRows(),
                    effectiveNegate
                );

            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ ->
                applyNotInNullGuard(
                    renderComputedInSubquery(inSubquery.left(), membershipSubquery.values(), rel, effectiveNegate),
                    membershipSubquery.nullRows(),
                    effectiveNegate
                );

            case IRExpression.ScalarSubquery _ ->
                throw unsupported("Scalar subquery on left side of IN");

            case IRExpression.Aggregate _ ->
                throw unsupported(inSubquery.left().getClass().getSimpleName());
        };
    }

    Optional<InlinedCorrelatedComparison> inlineCorrelatedComparison(
        Condition.Comparison comparison,
        String rel,
        RenderContext ctx
    ) {
        if (comparison.left() instanceof IRExpression.ScalarSubquery) {
            return Optional.empty();
        }
        if (!(comparison.right() instanceof IRExpression.ScalarSubquery subquery)
            || subquery.correlations().isEmpty()) {
            return Optional.empty();
        }
        if (ExpressionSqlRenderer.containsCaseWhen(comparison.left())) {
            return Optional.empty();
        }

        var alias = ctx.nextName("corr_subquery_");
        var correlatedRows = renderCorrelatedSubqueryRows(subquery, ctx);
        var subqueryFromItem = new ParenthesedSelect();
        subqueryFromItem.setSelect(correlatedRows);
        subqueryFromItem.setAlias(new Alias(alias, false));

        var predicates = new ArrayList<Expression>();
        predicates.add(Sql.comparison(
            ExpressionSqlRenderer.toSqlExpr(comparison.left(), rel, dialect),
            comparison.operator(),
            column(alias, "subquery_value")
        ));
        for (var correlation : subquery.correlations()) {
            predicates.add(comparison(
                column(attrTable(rel, correlation.outerAttribute()), "v"),
                correlation.operator(),
                column(alias, correlation.innerAttribute())
            ));
        }

        var requiredColumns = new ArrayList<String>();
        requiredColumns.addAll(ExpressionSqlRenderer.collectColumns(comparison.left()));
        requiredColumns.addAll(subquery.correlations().stream()
            .map(IRExpression.Correlation::outerAttribute)
            .toList());

        return java.util.Optional.of(new InlinedCorrelatedComparison(
            subqueryFromItem,
            predicates,
            requiredColumns.stream().distinct().toList()
        ));
    }

    PlainSelect renderExistsSubquery(IRNode subquery, RenderContext ctx) {
        var finalBaseName = subqueryRenderer.apply(subquery, ctx);
        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(table(idTable(finalBaseName)));
        return ps;
    }

    private Expression render(Condition.Comparison comp, String rel, boolean negate, RenderContext ctx) {
        return switch (comp.left()) {
            case IRExpression.ColumnRef(var col) ->
                renderWithColumn(col, comp.right(), comp.operator(), rel, negate, ctx);

            case IRExpression.Literal lit ->
                renderWithLiteral(lit, comp.right(), comp.operator(), rel, negate, ctx);

            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ ->
                switch (comp.right()) {
                    case IRExpression.ScalarSubquery subquery when !subquery.correlations().isEmpty() ->
                        existsExprToCorrelatedSubquery(
                            rel,
                            comp.left(),
                            comp.operator(),
                            subquery,
                            negate,
                            ctx
                        );
                    case IRExpression.ScalarSubquery _ ->
                        throw unsupported("Scalar subquery with computed expression on left side");
                    default -> renderWithComputedExpr(comp.left(), comp.right(), comp.operator(), rel, negate);
                };

            case IRExpression.ScalarSubquery _ ->
                throw unsupported("Scalar subquery on left side of comparison");

            case IRExpression.Aggregate _ ->
                throw unsupported(comp.left().getClass().getSimpleName());
        };
    }

    private Expression renderWithComputedExpr(IRExpression leftExpr, IRExpression right, String op,
                                              String rel, boolean negate) {
        var allCols = ExpressionSqlRenderer.collectColumns(leftExpr, right);

        return existsExprToExpr(rel, leftExpr, right, op, allCols, negate);
    }

    private Expression renderWithColumn(String col, IRExpression right, String op, String rel,
                                        boolean negate, RenderContext ctx) {
        return switch (right) {
            case IRExpression.ColumnRef(var rightCol) ->
                existsColumnToColumn(rel, col, rightCol, op, negate);

            case IRExpression.Literal lit ->
                existsColumnToLiteral(rel, col, op, lit, negate);

            case IRExpression.ScalarSubquery subq ->
                subq.correlations().isEmpty()
                    ? existsColumnToSubquery(
                        rel,
                        col,
                        op,
                        renderValueSubquery(subq, "Scalar subquery", ctx),
                        negate
                    )
                    : existsExprToCorrelatedSubquery(
                        rel,
                        new IRExpression.ColumnRef(col),
                        op,
                        subq,
                        negate,
                        ctx
                    );

            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ ->
                renderWithComputedExpr(new IRExpression.ColumnRef(col), right, op, rel, negate);

            case IRExpression.Aggregate _ ->
                throw unsupported(right.getClass().getSimpleName());
        };
    }

    private Expression renderWithLiteral(IRExpression.Literal lit, IRExpression right, String op,
                                         String rel, boolean negate, RenderContext ctx) {
        return switch (right) {
            case IRExpression.ColumnRef(var col) ->
                existsLiteralToColumn(rel, col, op, lit, negate);

            case IRExpression.Literal rightLit -> {
                var comp = comparison(literal(lit), op, literal(rightLit));
                yield negate ? not(paren(comp)) : comp;
            }

            case IRExpression.ScalarSubquery subq -> {
                if (subq.correlations().isEmpty()) {
                    var subSelect = renderValueSubquery(subq, "Scalar subquery", ctx);
                    var sub = new ParenthesedSelect();
                    sub.setSelect(subSelect);
                    yield comparison(literal(lit), op, sub);
                }
                yield existsExprToCorrelatedSubquery(rel, lit, op, subq, negate, ctx);
            }

            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ -> {
                var rightCols = ExpressionSqlRenderer.collectColumns(right);
                yield existsExprToExpr(rel, lit, right, op, rightCols, negate);
            }

            case IRExpression.Aggregate _ ->
                throw unsupported(right.getClass().getSimpleName());
        };
    }

    private Expression existsExprToExpr(String rel, IRExpression left, IRExpression right,
                                        String op, List<String> columns, boolean negate) {
        var hasCaseWhen = ExpressionSqlRenderer.containsCaseWhen(left)
            || ExpressionSqlRenderer.containsCaseWhen(right);

        var comp = comparison(
            ExpressionSqlRenderer.toSqlExpr(left, rel, dialect),
            op,
            ExpressionSqlRenderer.toSqlExpr(right, rel, dialect)
        );
        if (negate) {
            comp = not(paren(comp));
        }

        return renderExistsForPredicate(rel, comp, columns, hasCaseWhen);
    }

    private Expression renderComputedInSubquery(
        IRExpression leftExpr,
        PlainSelect subquery,
        String rel,
        boolean negate
    ) {
        var columns = ExpressionSqlRenderer.collectColumns(leftExpr);
        var hasCaseWhen = ExpressionSqlRenderer.containsCaseWhen(leftExpr);
        var predicate = inPredicate(ExpressionSqlRenderer.toSqlExpr(leftExpr, rel, dialect), subquery, negate);
        return renderExistsForPredicate(rel, predicate, columns, hasCaseWhen);
    }

    private Expression applyNotInNullGuard(
        Expression predicate,
        PlainSelect nullRows,
        boolean negate
    ) {
        return negate ? and(predicate, notExists(nullRows)) : predicate;
    }

    private Expression existsExprToCorrelatedSubquery(
        String rel,
        IRExpression leftExpr,
        String op,
        IRExpression.ScalarSubquery subquery,
        boolean negate,
        RenderContext ctx
    ) {
        if (subquery.subqueryPipeline().isEmpty()) {
            throw new IllegalStateException("Correlated scalar subquery has empty pipeline");
        }
        if (subquery.correlations().isEmpty()) {
            throw new IllegalStateException("Correlated scalar subquery is missing correlation metadata");
        }

        var correlatedAggregate = renderDirectCorrelatedAggregate(subquery, rel, ctx);
        if (correlatedAggregate.isPresent()) {
            return existsExprToCorrelatedAggregateSubquery(
                rel,
                leftExpr,
                op,
                correlatedAggregate.get(),
                subquery.correlations(),
                negate
            );
        }

        var valueAttribute = subquery.valueAttribute().orElseThrow(() -> new IllegalStateException(
            "Correlated scalar subquery is missing value attribute"
        ));
        var subqueryIR = subquery.subqueryPipeline().getFirst();
        var finalBaseName = subqueryRenderer.apply(subqueryIR, ctx);
        var valueTable = tableAlias(
            correlatedSubqueryAttrTable(subqueryIR, finalBaseName, valueAttribute),
            "corr_subquery_value"
        );

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());

        var outerIdTbl = table(idTable(rel));

        List<String> joinedColumns = new ArrayList<>();
        joinedColumns.addAll(ExpressionSqlRenderer.collectColumns(leftExpr));
        joinedColumns.addAll(subquery.correlations().stream()
            .map(IRExpression.Correlation::outerAttribute)
            .toList());
        joinedColumns = joinedColumns.stream().distinct().toList();
        if (joinedColumns.isEmpty()) {
            throw new IllegalStateException("Correlated scalar subquery requires at least one outer attribute");
        }

        var firstColumn = joinedColumns.getFirst();
        var firstTable = table(attrTable(rel, firstColumn));
        ps.setFromItem(firstTable);

        var joins = new ArrayList<Join>();
        var conditions = new ArrayList<Expression>();
        conditions.add(new EqualsTo(
            column(firstTable, "id"),
            column(outerIdTbl, "id")
        ));

        addComputedExprAttributeJoins(
            rel,
            joinedColumns.subList(1, joinedColumns.size()),
            column(outerIdTbl, "id"),
            false,
            Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
            joins,
            conditions
        );

        joins.add(simpleJoin(valueTable));

        var innerValueExprs = new LinkedHashMap<String, Expression>();
        innerValueExprs.put(valueAttribute, column(valueTable, "v"));

        var valueComparison = comparison(
            ExpressionSqlRenderer.toSqlExpr(leftExpr, rel, dialect),
            op,
            column(valueTable, "v")
        );
        if (negate) {
            valueComparison = not(paren(valueComparison));
        }
        conditions.add(valueComparison);

        for (var correlation : subquery.correlations()) {
            var innerValueExpr = innerValueExprs.computeIfAbsent(correlation.innerAttribute(), innerAttribute -> {
                var innerTable = tableAlias(
                    correlatedSubqueryAttrTable(subqueryIR, finalBaseName, innerAttribute),
                    "corr_subquery_attr_" + innerValueExprs.size()
                );
                joins.add(join(
                    innerTable,
                    new EqualsTo(
                        column(innerTable, "id"),
                        column(valueTable, "id")
                    )
                ));
                return column(innerTable, "v");
            });
            conditions.add(comparison(
                column(attrTable(rel, correlation.outerAttribute()), "v"),
                correlation.operator(),
                innerValueExpr
            ));
        }

        ps.setJoins(joins);
        ps.setWhere(andAll(conditions));
        return exists(ps);
    }

    private Expression existsExprToCorrelatedAggregateSubquery(
        String rel,
        IRExpression leftExpr,
        String op,
        Expression scalarAggregate,
        List<IRExpression.Correlation> correlations,
        boolean negate
    ) {
        var outerIdTbl = table(idTable(rel));
        var collectedColumns = new ArrayList<String>();
        collectedColumns.addAll(ExpressionSqlRenderer.collectColumns(leftExpr));
        collectedColumns.addAll(correlations.stream()
            .map(IRExpression.Correlation::outerAttribute)
            .toList());
        var joinedColumns = collectedColumns.stream().distinct().toList();
        if (joinedColumns.isEmpty()) {
            throw new IllegalStateException("Correlated scalar aggregate requires at least one outer attribute");
        }

        var firstTable = table(attrTable(rel, joinedColumns.getFirst()));
        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(firstTable);

        var joins = new ArrayList<Join>();
        var conditions = new ArrayList<Expression>();
        conditions.add(new EqualsTo(
            column(firstTable, "id"),
            column(outerIdTbl, "id")
        ));
        addComputedExprAttributeJoins(
            rel,
            joinedColumns.subList(1, joinedColumns.size()),
            column(outerIdTbl, "id"),
            false,
            Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
            joins,
            conditions
        );

        var valueComparison = comparison(
            ExpressionSqlRenderer.toSqlExpr(leftExpr, rel, dialect),
            op,
            scalarAggregate
        );
        if (negate) {
            valueComparison = not(paren(valueComparison));
        }
        conditions.add(valueComparison);

        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }
        ps.setWhere(andAll(conditions));
        return exists(ps);
    }

    private Optional<Expression> renderDirectCorrelatedAggregate(
        IRExpression.ScalarSubquery subquery,
        String outerBaseName,
        RenderContext ctx
    ) {
        if (subquery.subqueryPipeline().isEmpty() || subquery.valueAttribute().isNone()) {
            return Optional.empty();
        }
        if (subquery.correlations().stream().anyMatch(correlation -> !"=".equals(correlation.operator()))) {
            return Optional.empty();
        }

        var root = subquery.subqueryPipeline().getFirst();
        if (!(root instanceof Return returnNode)
            || !(returnNode.input() instanceof Group group)
            || !(group.input() instanceof Product product)
            || product.relations().size() != 1
            || !product.joinPredicates().isEmpty()) {
            return Optional.empty();
        }
        if (!(product.relations().getFirst() instanceof Relation.Table relation)) {
            return Optional.empty();
        }

        var correlationInnerAttributes = subquery.correlations().stream()
            .map(IRExpression.Correlation::innerAttribute)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!new HashSet<>(group.groupingAttributes()).equals(correlationInnerAttributes)) {
            return Optional.empty();
        }

        var valueAttribute = subquery.valueAttribute().get();
        var returnedValue = returnNode.selectedAttributes().stream()
            .filter(attribute -> attribute.alias().equals(valueAttribute))
            .findFirst();
        if (returnedValue.isEmpty()) {
            return Optional.empty();
        }

        var aggregateByAlias = new HashMap<String, IRExpression.Aggregate>();
        group.aggregates().forEach(aggregate -> aggregateByAlias.put(aggregate.alias(), aggregate));

        var innerIdAlias = ctx.nextName("corr_agg_id_");
        var attrAliases = new LinkedHashMap<String, String>();
        subquery.correlations().forEach(correlation ->
            attrAliases.computeIfAbsent(correlation.innerAttribute(), _ -> ctx.nextName("corr_agg_attr_"))
        );
        group.aggregates().stream()
            .flatMap(aggregate -> ExpressionSqlRenderer.collectColumns(aggregate.argument()).stream())
            .forEach(attribute -> attrAliases.computeIfAbsent(attribute, _ -> ctx.nextName("corr_agg_attr_")));

        var select = new PlainSelect();
        select.setFromItem(tableAs(relation.tableName() + "__ID", innerIdAlias));

        var joins = new ArrayList<Join>();
        attrAliases.forEach((attribute, alias) -> {
            var attrTable = tableAs(attrTable(relation.tableName(), unqualifiedAttribute(attribute, relation)), alias);
            var idComparison = new EqualsTo(column(attrTable, "id"), column(innerIdAlias, "id"));
            if (correlationInnerAttributes.contains(attribute)) {
                joins.add(join(attrTable, idComparison));
            } else {
                joins.add(leftJoin(attrTable, idComparison));
            }
        });
        if (!joins.isEmpty()) {
            select.setJoins(joins);
        }

        var correlationPredicates = subquery.correlations().stream()
            .map(correlation -> comparison(
                column(attrAliases.get(correlation.innerAttribute()), "v"),
                correlation.operator(),
                column(attrTable(outerBaseName, correlation.outerAttribute()), "v")
            ))
            .toList();
        select.setWhere(andAll(correlationPredicates));

        try {
            select.addSelectItem(renderAggregateValueExpression(
                returnedValue.get().source(),
                aggregateByAlias,
                attrAliases
            ));
        } catch (UnsupportedOperationException _) {
            return Optional.empty();
        }

        var scalarSelect = new ParenthesedSelect();
        scalarSelect.setSelect(select);
        return Optional.of(scalarSelect);
    }

    private Expression renderAggregateValueExpression(
        IRExpression expression,
        HashMap<String, IRExpression.Aggregate> aggregateByAlias,
        LinkedHashMap<String, String> attrAliases
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) -> {
                var aggregate = aggregateByAlias.get(columnName);
                if (aggregate == null) {
                    throw new UnsupportedOperationException("Only aggregate aliases are supported in scalar value");
                }
                yield renderAggregateFunction(aggregate, attrAliases);
            }
            case IRExpression.Literal literal -> literal(literal);
            case IRExpression.BinaryOp(var left, var operator, var right) ->
                arithmetic(
                    renderAggregateValueExpression(left, aggregateByAlias, attrAliases),
                    operator.toSql(),
                    renderAggregateValueExpression(right, aggregateByAlias, attrAliases)
                );
            case IRExpression.Cast(var inner, var targetType) ->
                new CastExpression(
                    "CAST",
                    renderAggregateValueExpression(inner, aggregateByAlias, attrAliases),
                    targetType
                );
            case IRExpression.FunctionCall _, IRExpression.CaseWhen _,
                 IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported correlated aggregate value expression");
        };
    }

    private Expression renderAggregateFunction(
        IRExpression.Aggregate aggregate,
        LinkedHashMap<String, String> attrAliases
    ) {
        var argument = renderAggregateArgumentExpression(aggregate.argument(), attrAliases);
        var aggregateFunction = fn(aggregate.function(), argument);
        aggregateFunction.setDistinct(aggregate.distinct());
        return aggregateFunction;
    }

    private Expression renderAggregateArgumentExpression(
        IRExpression expression,
        LinkedHashMap<String, String> attrAliases
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) -> column(attrAliases.get(columnName), "v");
            case IRExpression.Literal literal -> literal(literal);
            case IRExpression.BinaryOp(var left, var operator, var right) ->
                arithmetic(
                    renderAggregateArgumentExpression(left, attrAliases),
                    operator.toSql(),
                    renderAggregateArgumentExpression(right, attrAliases)
                );
            case IRExpression.Cast(var inner, var targetType) ->
                new CastExpression("CAST", renderAggregateArgumentExpression(inner, attrAliases), targetType);
            case IRExpression.FunctionCall _, IRExpression.CaseWhen _,
                 IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported correlated aggregate argument expression");
        };
    }

    private String unqualifiedAttribute(String qualifiedAttribute, Relation.Table relation) {
        var prefix = relation.alias() + "_";
        return qualifiedAttribute.startsWith(prefix)
            ? qualifiedAttribute.substring(prefix.length())
            : qualifiedAttribute;
    }

    private PlainSelect renderCorrelatedSubqueryRows(
        IRExpression.ScalarSubquery subquery,
        RenderContext ctx
    ) {
        if (subquery.subqueryPipeline().isEmpty()) {
            throw new IllegalStateException("Correlated scalar subquery has empty pipeline");
        }
        if (subquery.correlations().isEmpty()) {
            throw new IllegalStateException("Correlated scalar subquery is missing correlation metadata");
        }

        var valueAttribute = subquery.valueAttribute().orElseThrow(() -> new IllegalStateException(
            "Correlated scalar subquery is missing value attribute"
        ));

        var subqueryIR = subquery.subqueryPipeline().getFirst();
        var finalBaseName = subqueryRenderer.apply(subqueryIR, ctx);

        var valueTable = table(correlatedSubqueryAttrTable(subqueryIR, finalBaseName, valueAttribute));
        var ps = new PlainSelect();
        ps.setFromItem(valueTable);
        ps.addSelectItem(column(valueTable, "v"), new Alias("subquery_value", true));

        for (var correlation : subquery.correlations()) {
            var innerTable = table(correlatedSubqueryAttrTable(
                subqueryIR,
                finalBaseName,
                correlation.innerAttribute()
            ));
            ps.addSelectItem(column(innerTable, "v"), new Alias(correlation.innerAttribute(), true));
            ps.addJoins(join(
                innerTable,
                new EqualsTo(
                    column(innerTable, "id"),
                    column(valueTable, "id")
                )
            ));
        }

        return ps;
    }

    private String correlatedSubqueryAttrTable(IRNode subqueryIR, String finalBaseName, String attribute) {
        return subqueryIR instanceof Return
            ? attrCTE(finalBaseName, attribute)
            : attrTable(finalBaseName, attribute);
    }

    private Expression renderExistsForPredicate(
        String rel,
        Expression predicate,
        List<String> columns,
        boolean hasCaseWhen
    ) {
        if (columns.isEmpty()) {
            return predicate;
        }

        var idTbl = table(idTable(rel));
        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());

        if (hasCaseWhen) {
            var cwIdTbl = tableAlias(idTable(rel), "cw_id");
            ps.setFromItem(cwIdTbl);

            var joins = new ArrayList<Join>();
            addComputedExprAttributeJoins(
                rel,
                columns,
                column("cw_id", "id"),
                true,
                Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
                joins,
                new ArrayList<>()
            );
            ps.setJoins(joins);

            ps.setWhere(and(
                new EqualsTo(
                    column("cw_id", "id"),
                    column(idTbl, "id")
                ),
                predicate
            ));
        } else {
            var firstCol = columns.getFirst();
            var firstTable = table(attrTable(rel, firstCol));
            ps.setFromItem(firstTable);

            var joins = new ArrayList<Join>();
            var conditions = new ArrayList<Expression>();
            conditions.add(new EqualsTo(
                column(firstTable, "id"),
                column(idTbl, "id")
            ));
            addComputedExprAttributeJoins(
                rel,
                columns.subList(1, columns.size()),
                column(idTbl, "id"),
                false,
                Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
                joins,
                conditions
            );

            if (!joins.isEmpty()) {
                ps.setJoins(joins);
            }

            conditions.add(predicate);
            ps.setWhere(andAll(conditions));
        }

        return exists(ps);
    }

    private Expression existsColumnToColumn(String rel, String leftCol, String rightCol,
                                            String op, boolean negate) {
        var leftTable = table(attrTable(rel, leftCol));
        var rightTable = table(attrTable(rel, rightCol));
        var idTbl = table(idTable(rel));

        Expression comp = comparison(column(leftTable, "v"), op, column(rightTable, "v"));
        if (negate) {
            comp = not(paren(comp));
        }

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(leftTable);
        ps.addJoins(join(
            rightTable,
            new EqualsTo(
                column(rightTable, "id"),
                column(idTbl, "id")
            )
        ));
        ps.setWhere(andAll(List.of(
            new EqualsTo(
                column(leftTable, "id"),
                column(idTbl, "id")
            ),
            comp
        )));

        return exists(ps);
    }

    private Expression existsWithSingleAttr(String rel, String col, Expression comp, boolean negate) {
        if (negate) {
            comp = not(paren(comp));
        }

        var attrTbl = table(attrTable(rel, col));
        var idTbl = table(idTable(rel));

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(attrTbl);
        ps.setWhere(and(
            new EqualsTo(
                column(attrTbl, "id"),
                column(idTbl, "id")
            ),
            comp
        ));

        return exists(ps);
    }

    private Expression existsColumnToLiteral(String rel, String col, String op,
                                             IRExpression.Literal lit, boolean negate) {
        var comp = comparison(column(table(attrTable(rel, col)), "v"), op, literal(lit));
        return existsWithSingleAttr(rel, col, comp, negate);
    }

    private Expression existsColumnToSubquery(String rel, String col, String op,
                                              PlainSelect subquery, boolean negate) {
        var sub = new ParenthesedSelect();
        sub.setSelect(subquery);
        var comp = comparison(column(table(attrTable(rel, col)), "v"), op, sub);
        return existsWithSingleAttr(rel, col, comp, negate);
    }

    private Expression existsColumnInSubquery(String rel, String col, PlainSelect subquery, boolean negate) {
        var comp = inPredicate(column(table(attrTable(rel, col)), "v"), subquery, negate);
        return existsWithSingleAttr(rel, col, comp, false);
    }

    private Expression existsLiteralToColumn(String rel, String col, String op,
                                             IRExpression.Literal lit, boolean negate) {
        var comp = comparison(literal(lit), op, column(table(attrTable(rel, col)), "v"));
        return existsWithSingleAttr(rel, col, comp, negate);
    }

    private Expression inPredicate(Expression left, PlainSelect subquery, boolean negated) {
        var inExpression = new InExpression();
        inExpression.setLeftExpression(left);

        var right = new ParenthesedSelect();
        right.setSelect(subquery);
        inExpression.setRightExpression(right);
        inExpression.setNot(negated);
        return inExpression;
    }

    private PlainSelect renderValueSubquery(
        IRExpression.ScalarSubquery subquery,
        String subqueryType,
        RenderContext ctx
    ) {
        if (!subquery.correlations().isEmpty()) {
            throw new IllegalStateException(subqueryType + " must be uncorrelated");
        }
        if (subquery.subqueryPipeline().isEmpty()) {
            throw new IllegalStateException(subqueryType + " has empty pipeline");
        }

        var subqueryIR = subquery.subqueryPipeline().getFirst();
        return renderValueSubquery(subqueryIR, subqueryType, ctx);
    }

    private PlainSelect renderValueSubquery(IRNode subqueryIR, String subqueryType, RenderContext ctx) {
        return renderMembershipSubquery(subqueryIR, subqueryType, ctx).values();
    }

    private RenderedValueSubquery renderMembershipSubquery(
        IRNode subqueryIR,
        String subqueryType,
        RenderContext ctx
    ) {
        var finalBaseName = subqueryRenderer.apply(subqueryIR, ctx);

        var returnNode = IRNodeTraversal.findReturnNode(subqueryIR);
        if (returnNode == null || returnNode.selectStar()) {
            throw new IllegalStateException(subqueryType + " must have a single selected attribute");
        }

        var attrs = returnNode.selectedAttributes();
        if (attrs.size() != 1) {
            throw new IllegalStateException(
                subqueryType + " must return exactly one column, got: " + attrs.size()
            );
        }

        var attr = attrs.getFirst();
        var valueTable = table(attrCTE(finalBaseName, attr.alias()));

        var values = new PlainSelect();
        values.addSelectItem(column("v"));
        values.setFromItem(valueTable);

        var idTable = table(idTable(finalBaseName));
        var nullProbe = new PlainSelect();
        nullProbe.addSelectItem(new AllColumns());
        nullProbe.setFromItem(valueTable);
        nullProbe.setWhere(new EqualsTo(
            column(valueTable, "id"),
            column(idTable, "id")
        ));

        var nullRows = new PlainSelect();
        nullRows.addSelectItem(new AllColumns());
        nullRows.setFromItem(idTable);
        nullRows.setWhere(notExists(nullProbe));

        return new RenderedValueSubquery(values, nullRows);
    }

    private Optional<PlainSelect> renderDirectInMembershipSubquery(IRNode subqueryIR) {
        if (!(subqueryIR instanceof Return returnNode)
            || returnNode.selectStar()
            || returnNode.selectedAttributes().size() != 1
            || !(returnNode.selectedAttributes().getFirst().source() instanceof IRExpression.ColumnRef(var selectedColumn))
            || !(returnNode.input() instanceof AggFilter aggFilter)
            || !(aggFilter.input() instanceof Group group)
            || group.groupingAttributes().size() != 1
            || group.aggregates().size() != 1
            || !(group.input() instanceof Product product)
            || product.relations().size() != 1
            || !product.joinPredicates().isEmpty()
            || !(product.relations().getFirst() instanceof Relation.Table relation)) {
            return Optional.empty();
        }

        var groupingAttribute = group.groupingAttributes().getFirst();
        if (!selectedColumn.equals(groupingAttribute)) {
            return Optional.empty();
        }

        var aggregate = group.aggregates().getFirst();
        if (aggregate.argument() instanceof IRExpression.ColumnRef(var aggregateColumn)) {
            if (!allAttributesBelongToRelation(List.of(groupingAttribute, aggregateColumn), relation)) {
                return Optional.empty();
            }
            return renderDirectGroupedMembershipSubquery(
                relation,
                groupingAttribute,
                aggregateColumn,
                aggregate,
                aggFilter.condition()
            );
        }

        return Optional.empty();
    }

    private Optional<PlainSelect> renderDirectGroupedMembershipSubquery(
        Relation.Table relation,
        String groupingAttribute,
        String aggregateColumn,
        IRExpression.Aggregate aggregate,
        Condition havingCondition
    ) {
        var groupingAttrTbl = table(attrTable(relation.tableName(), unqualifiedAttribute(groupingAttribute, relation)));
        var aggregateAttrTbl = table(attrTable(relation.tableName(), unqualifiedAttribute(aggregateColumn, relation)));
        var aggregateExpr = fn(aggregate.function(), column(aggregateAttrTbl, "v"));
        aggregateExpr.setDistinct(aggregate.distinct());

        var having = renderDirectAggregateHaving(havingCondition, aggregate.alias(), aggregateExpr);
        if (having.isEmpty()) {
            return Optional.empty();
        }

        var ps = new PlainSelect();
        ps.addSelectItem(column(groupingAttrTbl, "v"));
        ps.setFromItem(groupingAttrTbl);
        var aggregateJoinCondition = new EqualsTo(
            column(aggregateAttrTbl, "id"),
            column(groupingAttrTbl, "id")
        );
        var aggregateJoin = aggregateReturnsNullForMissingArgument(aggregate)
            ? join(aggregateAttrTbl, aggregateJoinCondition)
            : leftJoin(aggregateAttrTbl, aggregateJoinCondition);
        ps.addJoins(aggregateJoin);
        ps.addGroupByColumnReference(column(groupingAttrTbl, "v"));
        ps.setHaving(having.get());
        return Optional.of(ps);
    }

    private boolean aggregateReturnsNullForMissingArgument(IRExpression.Aggregate aggregate) {
        return !"COUNT".equals(aggregate.function());
    }

    private Optional<Expression> renderDirectAggregateHaving(
        Condition condition,
        String aggregateAlias,
        Expression aggregateExpr
    ) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var operator) -> {
                var leftExpr = renderDirectAggregateHavingExpression(left, aggregateAlias, aggregateExpr);
                var rightExpr = renderDirectAggregateHavingExpression(right, aggregateAlias, aggregateExpr);
                if (leftExpr.isEmpty() || rightExpr.isEmpty()) {
                    yield Optional.empty();
                }
                yield Optional.of(comparison(leftExpr.get(), operator, rightExpr.get()));
            }
            case Condition.And(var operands) -> renderDirectAggregateHavingLogical(operands, aggregateAlias, aggregateExpr, true);
            case Condition.Or(var operands) -> renderDirectAggregateHavingLogical(operands, aggregateAlias, aggregateExpr, false);
            case Condition.Not(var operand) -> renderDirectAggregateHaving(operand, aggregateAlias, aggregateExpr)
                .map(expr -> not(paren(expr)));
            case Condition.IsNull _, Condition.Like _, Condition.Exists _, Condition.InSubquery _ -> Optional.empty();
        };
    }

    private Optional<Expression> renderDirectAggregateHavingLogical(
        List<Condition> operands,
        String aggregateAlias,
        Expression aggregateExpr,
        boolean conjunction
    ) {
        var rendered = new ArrayList<Expression>();
        for (var operand : operands) {
            var expression = renderDirectAggregateHaving(operand, aggregateAlias, aggregateExpr);
            if (expression.isEmpty()) {
                return Optional.empty();
            }
            rendered.add(paren(expression.get()));
        }
        return Optional.of(conjunction ? andAll(rendered) : orAll(rendered));
    }

    private Optional<Expression> renderDirectAggregateHavingExpression(
        IRExpression expression,
        String aggregateAlias,
        Expression aggregateExpr
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) when columnName.equals(aggregateAlias) ->
                Optional.of(aggregateExpr);
            case IRExpression.Literal literal -> Optional.of(literal(literal));
            case IRExpression.ColumnRef _, IRExpression.BinaryOp _, IRExpression.Cast _,
                 IRExpression.FunctionCall _, IRExpression.CaseWhen _,
                 IRExpression.Aggregate _, IRExpression.ScalarSubquery _ -> Optional.empty();
        };
    }

    private boolean allAttributesBelongToRelation(List<String> attributes, Relation.Table relation) {
        var sourceAttributes = new HashSet<>(relation.attributes());
        return attributes.stream()
            .map(attribute -> unqualifiedAttribute(attribute, relation))
            .allMatch(sourceAttributes::contains);
    }

    private UnsupportedOperationException unsupported(String type) {
        return new UnsupportedOperationException(type + " expressions not yet supported");
    }
}
