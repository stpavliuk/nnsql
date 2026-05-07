package nnsql.query.renderer.sql;

import nnsql.query.ir.Filter;
import nnsql.query.renderer.RenderContext;

import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

record FilterRenderer(ConditionRenderer conditionRenderer) {

    void render(Filter filter, RenderContext ctx, String baseName, String inputBaseName) {
        var optimizedFilterId = conditionRenderer.renderOptimizedFilterIdSelect(filter.condition(), inputBaseName, ctx);
        optimizedFilterId.ifPresentOrElse(
            optimized -> ctx.addCTE(idTable(baseName), optimized.select()),
            () -> addFilterIdCTE(ctx, baseName, inputBaseName,
                conditionRenderer.renderTrue(filter.condition(), inputBaseName, ctx))
        );
        var projectedAttributes = optimizedFilterId
            .map(ConditionRenderer.OptimizedFilterIdSelect::projectedAttributes)
            .orElse(List.of());
        addSelectivePassthroughAttributeCTEs(ctx, baseName, inputBaseName, filter.attributes(), projectedAttributes);
    }

    private void addSelectivePassthroughAttributeCTEs(
        RenderContext ctx,
        String baseName,
        String inputBaseName,
        List<String> attributes,
        List<String> projectedAttributes
    ) {
        attributes.forEach(attr -> {
            var baseIdTbl = table(idTable(baseName));
            var ps = new net.sf.jsqlparser.statement.select.PlainSelect();
            if (projectedAttributes.contains(attr)) {
                var projectedColumn = ConditionRenderer.projectedAttributeColumn(attr);
                ps.addSelectItem(column(baseIdTbl, "id"));
                ps.addSelectItem(column(baseIdTbl, projectedColumn), new net.sf.jsqlparser.expression.Alias("v", true));
                ps.setFromItem(baseIdTbl);
                ctx.addCTE(attrTable(baseName, attr), ps);
                return;
            }

            var inputAttrTbl = table(attrTable(inputBaseName, attr));
            ps.addSelectItem(new net.sf.jsqlparser.statement.select.AllTableColumns(inputAttrTbl));
            ps.setFromItem(baseIdTbl);
            ps.addJoins(join(
                inputAttrTbl,
                new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                    column(inputAttrTbl, "id"),
                    column(baseIdTbl, "id")
                )
            ));
            ctx.addCTE(attrTable(baseName, attr), ps);
        });
    }
}
