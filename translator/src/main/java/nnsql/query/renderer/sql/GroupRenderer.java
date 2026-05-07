package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.Condition;
import nnsql.query.ir.Filter;
import nnsql.query.ir.IRExpression;
import nnsql.query.ir.Group;
import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.renderer.RenderContext;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

class GroupRenderer {
    private final SqlDialect dialect;

    GroupRenderer(SqlDialect dialect) {
        this.dialect = dialect;
    }

    void render(Group group, RenderContext ctx, String baseName, String inputBaseName) {
        var groupedDataName = "grouped_" + baseName;
        if (!addDirectGlobalSumCTE(ctx, groupedDataName, group)
            && !addDirectFilteredProductGlobalSumCTE(ctx, groupedDataName, group)
            && !addDirectFilteredProductGroupCTE(ctx, groupedDataName, group)
            && !addDirectSingleTableFilteredGroupCTE(ctx, groupedDataName, group)) {
            addGroupedDataCTE(ctx, groupedDataName, inputBaseName, group);
        }
        addIdCTE(ctx, baseName, groupedDataName);
        addGroupingAttributeCTEs(ctx, baseName, groupedDataName, group.groupingAttributes());
        addAggregateCTEs(ctx, baseName, groupedDataName, group.aggregates());
    }

    private void addGroupedDataCTE(
        RenderContext ctx,
        String groupedDataName,
        String inputBaseName,
        Group group
    ) {
        var inputIdTbl = table(idTable(inputBaseName));
        var ps = new PlainSelect();
        ps.setFromItem(inputIdTbl);
        ps.addSelectItem(groupRepresentativeId(column(inputIdTbl, "id"), group), new Alias("id", true));

        var requiredColumns = new LinkedHashSet<>(group.groupingAttributes());
        group.aggregates().stream()
            .map(IRExpression.Aggregate::argument)
            .map(ExpressionSqlRenderer::collectColumns)
            .forEach(requiredColumns::addAll);

        requiredColumns.forEach(columnName -> {
            var attrTbl = table(attrTable(inputBaseName, columnName));
            ps.addJoins(leftJoin(attrTbl,
                new EqualsTo(column(attrTbl, "id"), column(inputIdTbl, "id"))));
        });

        var groupingProjections = java.util.stream.IntStream.range(0, group.groupingAttributes().size())
            .mapToObj(index -> groupingProjection(group.groupingAttributes().get(index), inputBaseName, index))
            .toList();

        for (var projection : groupingProjections) {
            ps.addSelectItem(projection.presentExpr(), new Alias(projection.presentAlias(), true));
            ps.addSelectItem(projection.valueExpr(), new Alias(projection.valueAlias(), true));
            ps.addGroupByColumnReference(projection.presentExpr());
            ps.addGroupByColumnReference(projection.valueExpr());
        }

        for (var aggregate : group.aggregates()) {
            var argumentExpr = ExpressionSqlRenderer.toSqlExpr(aggregate.argument(), inputBaseName, dialect);
            var aggregateFunction = fn(aggregate.function(), argumentExpr);
            aggregateFunction.setDistinct(aggregate.distinct());
            ps.addSelectItem(aggregateFunction, new Alias(aggregate.alias(), true));
        }

        ctx.addCTE(groupedDataName, ps);
    }

    private boolean addDirectSingleTableFilteredGroupCTE(
        RenderContext ctx,
        String groupedDataName,
        Group group
    ) {
        if (!dialect.optimizeSingleTableFilteredGroups()
            || group.groupingAttributes().isEmpty()
            || group.aggregates().isEmpty()
            || !(group.input() instanceof Filter filter)
            || !(filter.input() instanceof Product product)
            || product.relations().size() != 1
            || !product.joinPredicates().isEmpty()
            || !(product.relations().getFirst() instanceof Relation.Table relation)) {
            return false;
        }

        var filterColumns = ExpressionSqlRenderer.collectColumnsFromCondition(filter.condition());
        if (filterColumns.isEmpty()
            || !allAttributesBelongToRelation(filterColumns, relation)
            || !canRenderDirectCondition(filter.condition())) {
            return false;
        }

        if (group.aggregates().stream().map(IRExpression.Aggregate::argument).anyMatch(argument ->
            !canRenderDirectExpression(argument))) {
            return false;
        }

        var requiredColumns = new LinkedHashSet<>(filterColumns);
        requiredColumns.addAll(group.groupingAttributes());
        group.aggregates().stream()
            .map(IRExpression.Aggregate::argument)
            .map(ExpressionSqlRenderer::collectColumns)
            .forEach(requiredColumns::addAll);

        if (requiredColumns.isEmpty() || !allAttributesBelongToRelation(new ArrayList<>(requiredColumns), relation)) {
            return false;
        }

        var aliases = new LinkedHashMap<String, String>();
        var columns = new ArrayList<>(requiredColumns);
        for (int i = 0; i < columns.size(); i++) {
            aliases.put(columns.get(i), "direct_group_attr_" + i);
        }

        var anchorColumn = filterColumns.getFirst();
        var anchorAlias = aliases.get(anchorColumn);
        var ps = new PlainSelect();
        ps.setFromItem(tableAs(attrTable(relation.tableName(), unqualifiedAttribute(anchorColumn, relation)), anchorAlias));
        ps.addSelectItem(groupRepresentativeId(column(anchorAlias, "id"), group), new Alias("id", true));

        var joins = new ArrayList<Join>();
        for (var columnName : columns) {
            if (columnName.equals(anchorColumn)) {
                continue;
            }
            var alias = aliases.get(columnName);
            joins.add(leftJoin(
                tableAs(attrTable(relation.tableName(), unqualifiedAttribute(columnName, relation)), alias),
                new EqualsTo(column(alias, "id"), column(anchorAlias, "id"))
            ));
        }
        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }

        var predicate = renderDirectCondition(filter.condition(), aliases);
        if (predicate.isNone()) {
            return false;
        }
        ps.setWhere(predicate.get());

        var groupingProjections = java.util.stream.IntStream.range(0, group.groupingAttributes().size())
            .mapToObj(index -> directGroupingProjection(group.groupingAttributes().get(index), aliases, index))
            .toList();
        for (var projection : groupingProjections) {
            ps.addSelectItem(projection.presentExpr(), new Alias(projection.presentAlias(), true));
            ps.addSelectItem(projection.valueExpr(), new Alias(projection.valueAlias(), true));
            ps.addGroupByColumnReference(projection.valueExpr());
        }

        for (var aggregate : group.aggregates()) {
            var aggregateFunction = fn(aggregate.function(), renderDirectExpression(aggregate.argument(), aliases));
            aggregateFunction.setDistinct(aggregate.distinct());
            ps.addSelectItem(aggregateFunction, new Alias(aggregate.alias(), true));
        }

        ctx.addCTE(groupedDataName, ps);
        return true;
    }

    private boolean addDirectFilteredProductGroupCTE(
        RenderContext ctx,
        String groupedDataName,
        Group group
    ) {
        if (group.groupingAttributes().isEmpty()
            || group.aggregates().isEmpty()
            || group.aggregates().stream().anyMatch(aggregate ->
                !"SUM".equals(aggregate.function()) || aggregate.distinct())
            || !(group.input() instanceof Product product)
            || product.relations().size() <= 1
            || product.relations().size() > 4
            || product.joinPredicates().isEmpty()) {
            return false;
        }

        var directRelations = directProductRelations(product);
        if (directRelations.isNone()) {
            return false;
        }
        if (directRelations.get().stream().noneMatch(relation -> relation.filterCondition().isSome())) {
            return false;
        }

        if (group.aggregates().stream().map(IRExpression.Aggregate::argument).anyMatch(argument ->
            !canRenderDirectExpression(argument))) {
            return false;
        }

        var relations = directRelations.get();
        if (relations.stream().anyMatch(relation -> relation.filterCondition().isSome()
            && !canRenderDirectCondition(relation.filterCondition().get()))) {
            return false;
        }

        var presenceColumns = new LinkedHashSet<String>();
        var filterConditions = new ArrayList<Condition>();
        for (var relation : relations) {
            if (relation.filterCondition().isSome()) {
                var condition = relation.filterCondition().get();
                filterConditions.add(condition);
                presenceColumns.addAll(ExpressionSqlRenderer.collectColumnsFromCondition(condition));
            }
        }
        product.joinPredicates().forEach(predicate -> {
            presenceColumns.add(qualifiedJoinAttribute(product, predicate.leftRelIndex(), predicate.leftAttr()));
            presenceColumns.add(qualifiedJoinAttribute(product, predicate.rightRelIndex(), predicate.rightAttr()));
        });
        if (presenceColumns.isEmpty()) {
            return false;
        }

        var requiredColumns = new LinkedHashSet<>(presenceColumns);
        requiredColumns.addAll(group.groupingAttributes());
        group.aggregates().stream()
            .map(IRExpression.Aggregate::argument)
            .map(ExpressionSqlRenderer::collectColumns)
            .forEach(requiredColumns::addAll);

        var bindings = new LinkedHashMap<String, ColumnBinding>();
        for (var columnName : requiredColumns) {
            var binding = bindDirectProductColumn(relations, columnName);
            if (binding.isNone()) {
                return false;
            }
            bindings.put(columnName, binding.get());
        }

        var aliases = new LinkedHashMap<String, String>();
        var columns = new ArrayList<>(requiredColumns);
        for (int i = 0; i < columns.size(); i++) {
            aliases.put(columns.get(i), "direct_group_attr_" + i);
        }

        var anchorColumn = presenceColumns.getFirst();
        var anchorBinding = bindings.get(anchorColumn);
        var anchorAlias = aliases.get(anchorColumn);

        var ps = new PlainSelect();
        ps.setFromItem(tableAs(
            attrTable(anchorBinding.relation().tableName(), anchorBinding.sourceAttribute()),
            anchorAlias
        ));

        var joins = buildDirectProductJoins(
            product,
            columns,
            aliases,
            bindings,
            anchorColumn,
            presenceColumns
        );
        if (joins.isNone()) {
            return false;
        }
        if (!joins.get().joins().isEmpty()) {
            ps.setJoins(joins.get().joins());
        }

        var relationIdExpressions = new ArrayList<Expression>();
        for (int relationIndex = 0; relationIndex < product.relations().size(); relationIndex++) {
            var relationAnchor = joins.get().relationAnchors().get(relationIndex);
            if (relationAnchor == null) {
                return false;
            }
            relationIdExpressions.add(column(relationAnchor, "id"));
        }
        var productId = dialect.productRowIdExpressionRenderer().render(relationIdExpressions, product.nodeId());
        ps.addSelectItem(groupRepresentativeId(productId, group), new Alias("id", true));

        if (!filterConditions.isEmpty()) {
            var predicates = new ArrayList<Expression>();
            for (var condition : filterConditions) {
                var predicate = renderDirectCondition(condition, aliases);
                if (predicate.isNone()) {
                    return false;
                }
                predicates.add(paren(predicate.get()));
            }
            ps.setWhere(andAll(predicates));
        }

        var groupingProjections = java.util.stream.IntStream.range(0, group.groupingAttributes().size())
            .mapToObj(index -> directGroupingProjection(group.groupingAttributes().get(index), aliases, index))
            .toList();
        for (var projection : groupingProjections) {
            ps.addSelectItem(projection.presentExpr(), new Alias(projection.presentAlias(), true));
            ps.addSelectItem(projection.valueExpr(), new Alias(projection.valueAlias(), true));
            ps.addGroupByColumnReference(projection.valueExpr());
        }

        for (var aggregate : group.aggregates()) {
            var aggregateFunction = fn(aggregate.function(), renderDirectExpression(aggregate.argument(), aliases));
            ps.addSelectItem(aggregateFunction, new Alias(aggregate.alias(), true));
        }

        ctx.addCTE(groupedDataName, ps);
        return true;
    }

    private boolean addDirectGlobalSumCTE(
        RenderContext ctx,
        String groupedDataName,
        Group group
    ) {
        if (!group.groupingAttributes().isEmpty()
            || group.aggregates().isEmpty()
            || group.aggregates().stream().anyMatch(aggregate ->
                !"SUM".equals(aggregate.function()) || aggregate.distinct())) {
            return false;
        }
        if (!(group.input() instanceof Filter filter)
            || !(filter.input() instanceof Product product)
            || product.relations().size() != 1
            || !product.joinPredicates().isEmpty()
            || !(product.relations().getFirst() instanceof Relation.Table relation)) {
            return false;
        }

        var requiredColumns = new LinkedHashSet<>(
            ExpressionSqlRenderer.collectColumnsFromCondition(filter.condition())
        );
        group.aggregates().stream()
            .map(IRExpression.Aggregate::argument)
            .map(ExpressionSqlRenderer::collectColumns)
            .forEach(requiredColumns::addAll);
        if (requiredColumns.isEmpty()) {
            return false;
        }

        var aliases = new LinkedHashMap<String, String>();
        var columns = new ArrayList<>(requiredColumns);
        for (int i = 0; i < columns.size(); i++) {
            aliases.put(columns.get(i), "direct_group_attr_" + i);
        }

        var anchorColumn = columns.getFirst();
        var anchorAlias = aliases.get(anchorColumn);
        var ps = new PlainSelect();
        ps.setFromItem(tableAs(attrTable(relation.tableName(), unqualifiedAttribute(anchorColumn, relation)), anchorAlias));
        ps.addSelectItem(groupRepresentativeId(column(anchorAlias, "id"), group), new Alias("id", true));

        var joins = new ArrayList<Join>();
        for (var columnName : columns.subList(1, columns.size())) {
            var alias = aliases.get(columnName);
            joins.add(join(
                tableAs(attrTable(relation.tableName(), unqualifiedAttribute(columnName, relation)), alias),
                new EqualsTo(column(alias, "id"), column(anchorAlias, "id"))
            ));
        }
        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }

        var predicate = renderDirectCondition(filter.condition(), aliases);
        if (predicate.isNone()) {
            return false;
        }
        ps.setWhere(predicate.get());

        for (var aggregate : group.aggregates()) {
            var aggregateFunction = fn(aggregate.function(), renderDirectExpression(aggregate.argument(), aliases));
            ps.addSelectItem(aggregateFunction, new Alias(aggregate.alias(), true));
        }

        ctx.addCTE(groupedDataName, ps);
        return true;
    }

    private boolean addDirectFilteredProductGlobalSumCTE(
        RenderContext ctx,
        String groupedDataName,
        Group group
    ) {
        if (!group.groupingAttributes().isEmpty()
            || group.aggregates().isEmpty()
            || group.aggregates().stream().anyMatch(aggregate ->
                !"SUM".equals(aggregate.function()) || aggregate.distinct())) {
            return false;
        }

        var filteredProduct = filteredProductInput(group.input());
        if (filteredProduct.isNone()) {
            return false;
        }
        var product = filteredProduct.get().product();
        var outerCondition = filteredProduct.get().condition();
        if (product.relations().size() <= 1
            || product.relations().size() > 4
            || product.joinPredicates().isEmpty()
            || outerCondition.stream().anyMatch(condition -> !canRenderDirectCondition(condition))) {
            return false;
        }

        var directRelations = directProductRelations(product);
        if (directRelations.isNone()) {
            return false;
        }
        if (outerCondition.isNone()
            && directRelations.get().stream().noneMatch(relation -> relation.filterCondition().isSome())) {
            return false;
        }
        if (directRelations.get().stream().anyMatch(relation -> relation.filterCondition().isSome()
            && !canRenderDirectCondition(relation.filterCondition().get()))) {
            return false;
        }

        if (group.aggregates().stream().map(IRExpression.Aggregate::argument).anyMatch(argument ->
            !canRenderDirectExpression(argument))) {
            return false;
        }

        var presenceColumns = new LinkedHashSet<String>();
        outerCondition.stream()
            .map(ExpressionSqlRenderer::collectColumnsFromCondition)
            .forEach(presenceColumns::addAll);
        for (var relation : directRelations.get()) {
            if (relation.filterCondition().isSome()) {
                presenceColumns.addAll(ExpressionSqlRenderer.collectColumnsFromCondition(relation.filterCondition().get()));
            }
        }
        if (presenceColumns.isEmpty()) {
            return false;
        }

        var innerJoinColumns = new LinkedHashSet<>(presenceColumns);
        product.joinPredicates().forEach(predicate -> {
            innerJoinColumns.add(qualifiedJoinAttribute(product, predicate.leftRelIndex(), predicate.leftAttr()));
            innerJoinColumns.add(qualifiedJoinAttribute(product, predicate.rightRelIndex(), predicate.rightAttr()));
        });
        var requiredColumns = new LinkedHashSet<>(innerJoinColumns);
        group.aggregates().stream()
            .map(IRExpression.Aggregate::argument)
            .map(ExpressionSqlRenderer::collectColumns)
            .forEach(requiredColumns::addAll);
        if (requiredColumns.isEmpty()) {
            return false;
        }

        var bindings = new LinkedHashMap<String, ColumnBinding>();
        for (var columnName : requiredColumns) {
            var binding = bindDirectProductColumn(directRelations.get(), columnName);
            if (binding.isNone()) {
                return false;
            }
            bindings.put(columnName, binding.get());
        }

        var aliases = new LinkedHashMap<String, String>();
        var columns = new ArrayList<>(requiredColumns);
        for (int i = 0; i < columns.size(); i++) {
            aliases.put(columns.get(i), "direct_group_attr_" + i);
        }

        var anchorColumn = presenceColumns.getFirst();
        var anchorBinding = bindings.get(anchorColumn);
        var anchorAlias = aliases.get(anchorColumn);

        var ps = new PlainSelect();
        ps.setFromItem(tableAs(
            attrTable(anchorBinding.relation().tableName(), anchorBinding.sourceAttribute()),
            anchorAlias
        ));
        ps.addSelectItem(groupRepresentativeId(column(anchorAlias, "id"), group), new Alias("id", true));

        var joins = buildDirectProductJoins(product, columns, aliases, bindings, anchorColumn, innerJoinColumns);
        if (joins.isNone()) {
            return false;
        }
        if (!joins.get().joins().isEmpty()) {
            ps.setJoins(joins.get().joins());
        }

        var predicates = new ArrayList<Expression>();
        if (outerCondition.isSome()) {
            var predicate = renderDirectCondition(outerCondition.get(), aliases);
            if (predicate.isNone()) {
                return false;
            }
            predicates.add(paren(predicate.get()));
        }
        for (var relation : directRelations.get()) {
            if (relation.filterCondition().isSome()) {
                var localPredicate = renderDirectCondition(relation.filterCondition().get(), aliases);
                if (localPredicate.isNone()) {
                    return false;
                }
                predicates.add(paren(localPredicate.get()));
            }
        }
        ps.setWhere(andAll(predicates));

        for (var aggregate : group.aggregates()) {
            ps.addSelectItem(
                fn(aggregate.function(), renderDirectExpression(aggregate.argument(), aliases)),
                new Alias(aggregate.alias(), true)
            );
        }

        ctx.addCTE(groupedDataName, ps);
        return true;
    }

    private Option<FilteredProductInput> filteredProductInput(nnsql.query.ir.IRNode input) {
        return switch (input) {
            case Filter(var filterInput, var condition, _) when filterInput instanceof Product product ->
                Option.some(new FilteredProductInput(product, Option.some(condition)));
            case Product product -> Option.some(new FilteredProductInput(product, Option.none()));
            default -> Option.none();
        };
    }

    private Option<DirectProductJoins> buildDirectProductJoins(
        Product product,
        List<String> columns,
        LinkedHashMap<String, String> aliases,
        LinkedHashMap<String, ColumnBinding> bindings,
        String anchorColumn,
        LinkedHashSet<String> presenceColumns
    ) {
        var joins = new ArrayList<Join>();
        var joinedColumns = new HashSet<String>();
        var relationAnchors = new HashMap<Integer, String>();

        joinedColumns.add(anchorColumn);
        relationAnchors.put(bindings.get(anchorColumn).relationIndex(), aliases.get(anchorColumn));

        while (joinedColumns.size() < columns.size()) {
            var progressed = false;

            for (var columnName : columns) {
                if (joinedColumns.contains(columnName)) {
                    continue;
                }
                var binding = bindings.get(columnName);
                var relationAnchor = relationAnchors.get(binding.relationIndex());
                if (relationAnchor == null) {
                    continue;
                }

                var alias = aliases.get(columnName);
                var attrTable = tableAs(attrTable(binding.relation().tableName(), binding.sourceAttribute()), alias);
                var idEquals = new EqualsTo(column(alias, "id"), column(relationAnchor, "id"));
                joins.add(presenceColumns.contains(columnName)
                    ? join(attrTable, idEquals)
                    : leftJoin(attrTable, idEquals));
                joinedColumns.add(columnName);
                progressed = true;
            }

            if (progressed) {
                continue;
            }

            var joinBridge = nextJoinBridge(product, columns, aliases, bindings, joinedColumns);
            if (joinBridge.isNone()) {
                return Option.none();
            }

            var bridge = joinBridge.get();
            joins.add(join(
                tableAs(attrTable(bridge.newColumn().relation().tableName(), bridge.newColumn().sourceAttribute()),
                    bridge.newAlias()),
                new EqualsTo(column(bridge.existingAlias(), "v"), column(bridge.newAlias(), "v"))
            ));
            joinedColumns.add(bridge.newColumnName());
            relationAnchors.put(bridge.newColumn().relationIndex(), bridge.newAlias());
        }

        return Option.some(new DirectProductJoins(joins, relationAnchors));
    }

    private Option<JoinBridge> nextJoinBridge(
        Product product,
        List<String> columns,
        LinkedHashMap<String, String> aliases,
        LinkedHashMap<String, ColumnBinding> bindings,
        HashSet<String> joinedColumns
    ) {
        for (var predicate : product.joinPredicates()) {
            var leftColumn = qualifiedJoinAttribute(product, predicate.leftRelIndex(), predicate.leftAttr());
            var rightColumn = qualifiedJoinAttribute(product, predicate.rightRelIndex(), predicate.rightAttr());

            var leftJoined = joinedColumns.contains(leftColumn);
            var rightJoined = joinedColumns.contains(rightColumn);
            if (leftJoined == rightJoined || !columns.contains(leftColumn) || !columns.contains(rightColumn)) {
                continue;
            }

            var newColumn = leftJoined ? rightColumn : leftColumn;
            var existingColumn = leftJoined ? leftColumn : rightColumn;
            return Option.some(new JoinBridge(
                newColumn,
                bindings.get(newColumn),
                aliases.get(newColumn),
                aliases.get(existingColumn)
            ));
        }

        return Option.none();
    }

    private String qualifiedJoinAttribute(Product product, int relationIndex, String attribute) {
        var relation = product.relations().get(relationIndex);
        return relation.alias() + "_" + attribute;
    }

    private Option<ColumnBinding> bindProductColumn(Product product, String columnName) {
        for (int index = 0; index < product.relations().size(); index++) {
            if (!(product.relations().get(index) instanceof Relation.Table relation)) {
                return Option.none();
            }

            var sourceAttribute = unqualifiedAttribute(columnName, relation);
            if (relation.attributes().contains(sourceAttribute)) {
                return Option.some(new ColumnBinding(index, relation, sourceAttribute));
            }
        }

        return Option.none();
    }

    private Option<List<DirectRelation>> directProductRelations(Product product) {
        var relations = new ArrayList<DirectRelation>();
        for (int index = 0; index < product.relations().size(); index++) {
            var relation = directProductRelation(index, product.relations().get(index));
            if (relation.isNone()) {
                return Option.none();
            }
            relations.add(relation.get());
        }
        return Option.some(relations);
    }

    private Option<DirectRelation> directProductRelation(int relationIndex, Relation relation) {
        return switch (relation) {
            case Relation.Table table ->
                Option.some(new DirectRelation(relationIndex, table.alias(), table, Option.none()));
            case Relation.Subquery(var alias, var ir, _) -> directFilteredUnaryTable(ir)
                .map(filtered -> new DirectRelation(relationIndex, alias, filtered.relation(), Option.some(filtered.condition())));
        };
    }

    private Option<FilteredTable> directFilteredUnaryTable(nnsql.query.ir.IRNode node) {
        if (!(node instanceof nnsql.query.ir.Return ret)
            || !(ret.input() instanceof Filter filter)
            || !(filter.input() instanceof Product product)
            || product.relations().size() != 1
            || !product.joinPredicates().isEmpty()
            || !(product.relations().getFirst() instanceof Relation.Table relation)
            || !returnKeepsBaseColumns(ret, relation)) {
            return Option.none();
        }

        return Option.some(new FilteredTable(relation, filter.condition()));
    }

    private boolean returnKeepsBaseColumns(nnsql.query.ir.Return ret, Relation.Table relation) {
        if (ret.selectStar()) {
            return true;
        }

        for (var attribute : ret.selectedAttributes()) {
            if (!(attribute instanceof nnsql.query.ir.Return.ColumnAttributeRef(var source, var alias))
                || !alias.equals(unqualifiedAttribute(source.columnName(), relation))
                || !relation.attributes().contains(alias)) {
                return false;
            }
        }
        return true;
    }

    private Option<ColumnBinding> bindDirectProductColumn(List<DirectRelation> relations, String columnName) {
        for (var relation : relations) {
            var sourceAttribute = unqualifiedAttribute(columnName, relation);
            if (relation.relation().attributes().contains(sourceAttribute)) {
                return Option.some(new ColumnBinding(relation.relationIndex(), relation.relation(), sourceAttribute));
            }
        }

        return Option.none();
    }

    private String unqualifiedAttribute(String qualifiedAttribute, DirectRelation relation) {
        var outputPrefix = relation.outputAlias() + "_";
        if (qualifiedAttribute.startsWith(outputPrefix)) {
            return qualifiedAttribute.substring(outputPrefix.length());
        }
        return unqualifiedAttribute(qualifiedAttribute, relation.relation());
    }

    private Expression groupRepresentativeId(Expression inputIdExpression, Group group) {
        var representativeId = dialect.representativeId(inputIdExpression);
        return group.groupingAttributes().isEmpty()
            ? fn("COALESCE", representativeId, dialect.globalAggregateGroupId())
            : representativeId;
    }

    private Option<Expression> renderDirectCondition(
        Condition condition,
        LinkedHashMap<String, String> aliases
    ) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var operator) ->
                Option.some(comparison(
                    renderDirectExpression(left, aliases),
                    operator,
                    renderDirectExpression(right, aliases)
                ));
            case Condition.Like(var left, var pattern, var isNegated) ->
                Option.some(like(
                    renderDirectExpression(left, aliases),
                    renderDirectExpression(pattern, aliases),
                    isNegated
                ));
            case Condition.And(var operands) -> renderDirectLogicalCondition(operands, aliases, true);
            case Condition.Or(var operands) -> renderDirectLogicalCondition(operands, aliases, false);
            case Condition.Not(var operand) -> renderDirectCondition(operand, aliases)
                .map(expr -> not(paren(expr)));
            case Condition.IsNull _, Condition.Exists _, Condition.InSubquery _ -> Option.none();
        };
    }

    private Option<Expression> renderDirectLogicalCondition(
        List<Condition> operands,
        LinkedHashMap<String, String> aliases,
        boolean conjunction
    ) {
        var rendered = new ArrayList<Expression>();
        for (var operand : operands) {
            var expression = renderDirectCondition(operand, aliases);
            if (expression.isNone()) {
                return Option.none();
            }
            rendered.add(paren(expression.get()));
        }
        return Option.some(conjunction ? andAll(rendered) : orAll(rendered));
    }

    private boolean canRenderDirectCondition(Condition condition) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, _) ->
                canRenderDirectExpression(left) && canRenderDirectExpression(right);
            case Condition.Like(var left, var pattern, _) ->
                canRenderDirectExpression(left) && canRenderDirectExpression(pattern);
            case Condition.And(var operands) ->
                operands.stream().allMatch(this::canRenderDirectCondition);
            case Condition.Or(var operands) ->
                operands.stream().allMatch(this::canRenderDirectCondition);
            case Condition.Not(var operand) -> canRenderDirectCondition(operand);
            case Condition.IsNull _, Condition.Exists _, Condition.InSubquery _ -> false;
        };
    }

    private boolean canRenderDirectExpression(IRExpression expression) {
        return switch (expression) {
            case IRExpression.ColumnRef _, IRExpression.Literal _ -> true;
            case IRExpression.BinaryOp(var left, _, var right) ->
                canRenderDirectExpression(left) && canRenderDirectExpression(right);
            case IRExpression.Cast(var inner, _) -> canRenderDirectExpression(inner);
            case IRExpression.FunctionCall(_, var arguments) ->
                arguments.stream().allMatch(this::canRenderDirectExpression);
            case IRExpression.CaseWhen(var whens, var elseExpr) ->
                whens.stream().allMatch(when ->
                    canRenderDirectCondition(when.condition()) && canRenderDirectExpression(when.result()))
                    && elseExpr.stream().allMatch(this::canRenderDirectExpression);
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ -> false;
        };
    }

    private Expression renderDirectExpression(
        IRExpression expression,
        LinkedHashMap<String, String> aliases
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) -> column(aliases.get(columnName), "v");
            case IRExpression.Literal literal -> literal(literal);
            case IRExpression.BinaryOp binaryOp -> renderDirectBinaryOp(binaryOp, aliases);
            case IRExpression.Cast(var inner, var targetType) ->
                new CastExpression("CAST", renderDirectExpression(inner, aliases), targetType);
            case IRExpression.FunctionCall(var name, var arguments) ->
                dialect.renderFunction(
                    name,
                    arguments.stream()
                        .map(argument -> renderDirectExpression(argument, aliases))
                        .toList()
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) ->
                renderDirectCaseWhen(whens, elseExpr, aliases);
            case IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported expression in direct aggregate");
        };
    }

    private Expression renderDirectCaseWhen(
        List<IRExpression.WhenClause> whens,
        Option<IRExpression> elseExpr,
        LinkedHashMap<String, String> aliases
    ) {
        var caseExpr = new CaseExpression();
        caseExpr.setWhenClauses(whens.stream()
            .map(when -> {
                var sqlWhen = new WhenClause();
                var condition = renderDirectCondition(when.condition(), aliases);
                if (condition.isNone()) {
                    throw new UnsupportedOperationException("Unsupported CASE condition in direct aggregate");
                }
                sqlWhen.setWhenExpression(condition.get());
                sqlWhen.setThenExpression(renderDirectExpression(when.result(), aliases));
                return sqlWhen;
            })
            .toList());
        elseExpr.stream()
            .map(expr -> renderDirectExpression(expr, aliases))
            .forEach(caseExpr::setElseExpression);
        return caseExpr;
    }

    private Expression renderDirectBinaryOp(
        IRExpression.BinaryOp binaryOp,
        LinkedHashMap<String, String> aliases
    ) {
        var left = renderDirectExpression(binaryOp.left(), aliases);
        var right = renderDirectExpression(binaryOp.right(), aliases);

        if (needsParentheses(binaryOp.operator(), binaryOp.left(), false)) {
            left = paren(left);
        }
        if (needsParentheses(binaryOp.operator(), binaryOp.right(), true)) {
            right = paren(right);
        }

        return arithmetic(left, binaryOp.operator().toSql(), right);
    }

    private boolean needsParentheses(
        IRExpression.ArithmeticOperator parentOperator,
        IRExpression childExpression,
        boolean isRightChild
    ) {
        if (!(childExpression instanceof IRExpression.BinaryOp(_, var childOperator, _))) {
            return false;
        }

        var parentPrecedence = precedence(parentOperator);
        var childPrecedence = precedence(childOperator);

        if (childPrecedence < parentPrecedence) {
            return true;
        }
        if (childPrecedence > parentPrecedence || !isRightChild) {
            return false;
        }

        return switch (parentOperator) {
            case IRExpression.Add _ -> false;
            case IRExpression.Subtract _ -> true;
            case IRExpression.Multiply _ -> childOperator instanceof IRExpression.Divide;
            case IRExpression.Divide _ -> true;
        };
    }

    private int precedence(IRExpression.ArithmeticOperator operator) {
        return switch (operator) {
            case IRExpression.Add _, IRExpression.Subtract _ -> 1;
            case IRExpression.Multiply _, IRExpression.Divide _ -> 2;
        };
    }

    private String unqualifiedAttribute(String qualifiedAttribute, Relation.Table relation) {
        var prefix = relation.alias() + "_";
        return qualifiedAttribute.startsWith(prefix)
            ? qualifiedAttribute.substring(prefix.length())
            : qualifiedAttribute;
    }

    private boolean allAttributesBelongToRelation(List<String> attributes, Relation.Table relation) {
        var sourceAttributes = new java.util.HashSet<>(relation.attributes());
        return attributes.stream()
            .map(attribute -> unqualifiedAttribute(attribute, relation))
            .allMatch(sourceAttributes::contains);
    }

    private void addIdCTE(RenderContext ctx, String baseName, String groupedDataName) {
        var groupedDataTbl = table(groupedDataName);
        var ps = new PlainSelect();
        ps.addSelectItem(column(groupedDataTbl, "id"));
        ps.setFromItem(groupedDataTbl);
        ctx.addCTE(idTable(baseName), ps);
    }

    private void addGroupingAttributeCTEs(
        RenderContext ctx,
        String baseName,
        String groupedDataName,
        List<String> groupingAttributes
    ) {
        java.util.stream.IntStream.range(0, groupingAttributes.size())
            .forEach(index -> {
                var projection = groupingProjection(groupingAttributes.get(index), groupedDataName, index);
                var groupedDataTbl = table(groupedDataName);
                var ps = new PlainSelect();
                ps.addSelectItem(column(groupedDataTbl, "id"));
                ps.addSelectItem(column(groupedDataTbl, projection.valueAlias()), new Alias("v", true));
                ps.setFromItem(groupedDataTbl);
                ps.setWhere(new EqualsTo(
                    column(groupedDataTbl, projection.presentAlias()),
                    new LongValue(1)
                ));
                ctx.addCTE(attrTable(baseName, groupingAttributes.get(index)), ps);
            });
    }

    private void addAggregateCTEs(
        RenderContext ctx,
        String baseName,
        String groupedDataName,
        List<IRExpression.Aggregate> aggregates
    ) {
        aggregates.forEach(aggregate -> {
            var groupedDataTbl = table(groupedDataName);
            var ps = new PlainSelect();
            ps.addSelectItem(column(groupedDataTbl, "id"));
            ps.addSelectItem(column(groupedDataTbl, aggregate.alias()), new Alias("v", true));
            ps.setFromItem(groupedDataTbl);

            if (!"COUNT".equals(aggregate.function())) {
                var isNotNull = new IsNullExpression();
                isNotNull.setLeftExpression(column(groupedDataTbl, aggregate.alias()));
                isNotNull.setNot(true);
                ps.setWhere(isNotNull);
            }

            ctx.addCTE(attrTable(baseName, aggregate.alias()), ps);
        });
    }

    private GroupingProjection groupingProjection(String attribute, String relationName, int index) {
        var attrTbl = table(attrTable(relationName, attribute));
        var presentExpr = new CaseExpression()
            .withWhenClauses(List.of(new WhenClause(
                new IsNullExpression().withLeftExpression(column(attrTbl, "id")),
                new LongValue(0)
            )))
            .withElseExpression(new LongValue(1));
        return new GroupingProjection(
            "group_key_%d_present".formatted(index),
            "group_key_%d_value".formatted(index),
            presentExpr,
            column(attrTbl, "v")
        );
    }

    private GroupingProjection directGroupingProjection(
        String attribute,
        LinkedHashMap<String, String> aliases,
        int index
    ) {
        var attributeAlias = aliases.get(attribute);
        var presentExpr = fn("MAX", new CaseExpression()
            .withWhenClauses(List.of(new WhenClause(
                new IsNullExpression().withLeftExpression(column(attributeAlias, "id")),
                new LongValue(0)
            )))
            .withElseExpression(new LongValue(1)));
        return new GroupingProjection(
            "group_key_%d_present".formatted(index),
            "group_key_%d_value".formatted(index),
            presentExpr,
            column(attributeAlias, "v")
        );
    }

    private record GroupingProjection(
        String presentAlias,
        String valueAlias,
        Expression presentExpr,
        Expression valueExpr
    ) {
    }

    private record ColumnBinding(
        int relationIndex,
        Relation.Table relation,
        String sourceAttribute
    ) {
    }

    private record DirectRelation(
        int relationIndex,
        String outputAlias,
        Relation.Table relation,
        Option<Condition> filterCondition
    ) {
    }

    private record FilteredTable(
        Relation.Table relation,
        Condition condition
    ) {
    }

    private record FilteredProductInput(
        Product product,
        Option<Condition> condition
    ) {
    }

    private record DirectProductJoins(
        List<Join> joins,
        HashMap<Integer, String> relationAnchors
    ) {
    }

    private record JoinBridge(
        String newColumnName,
        ColumnBinding newColumn,
        String newAlias,
        String existingAlias
    ) {
    }
}
