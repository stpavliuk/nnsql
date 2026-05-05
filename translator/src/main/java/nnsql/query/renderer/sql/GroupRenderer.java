package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.IRExpression;
import nnsql.query.ir.Group;
import nnsql.query.renderer.RenderContext;

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
        addGroupedDataCTE(ctx, groupedDataName, inputBaseName, group);
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
