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
        if (!addDirectGlobalSumCTE(ctx, groupedDataName, group)) {
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
        ps.addSelectItem(dialect.representativeId(column(inputIdTbl, "id")), new Alias("id", true));

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

        var requiredColumns = new LinkedHashSet<String>();
        requiredColumns.addAll(ExpressionSqlRenderer.collectColumnsFromCondition(filter.condition()));
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
        ps.addSelectItem(dialect.representativeId(column(anchorAlias, "id")), new Alias("id", true));

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

    private Expression renderDirectExpression(
        IRExpression expression,
        LinkedHashMap<String, String> aliases
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef(var columnName) -> column(aliases.get(columnName), "v");
            case IRExpression.Literal literal -> literal(literal);
            case IRExpression.BinaryOp(var left, var operator, var right) ->
                arithmetic(
                    renderDirectExpression(left, aliases),
                    operator.toSql(),
                    renderDirectExpression(right, aliases)
                );
            case IRExpression.Cast(var inner, var targetType) ->
                new CastExpression("CAST", renderDirectExpression(inner, aliases), targetType);
            case IRExpression.FunctionCall(var name, var arguments) ->
                dialect.renderFunction(
                    name,
                    arguments.stream()
                        .map(argument -> renderDirectExpression(argument, aliases))
                        .toList()
                );
            case IRExpression.CaseWhen _, IRExpression.Aggregate _, IRExpression.ScalarSubquery _ ->
                throw new UnsupportedOperationException("Unsupported expression in direct aggregate");
        };
    }

    private String unqualifiedAttribute(String qualifiedAttribute, Relation.Table relation) {
        var prefix = relation.alias() + "_";
        return qualifiedAttribute.startsWith(prefix)
            ? qualifiedAttribute.substring(prefix.length())
            : qualifiedAttribute;
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

    private record GroupingProjection(
        String presentAlias,
        String valueAlias,
        Expression presentExpr,
        Expression valueExpr
    ) {
    }
}
