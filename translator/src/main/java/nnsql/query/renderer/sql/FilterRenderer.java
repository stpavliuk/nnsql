package nnsql.query.renderer.sql;

import nnsql.query.ir.Filter;
import nnsql.query.renderer.RenderContext;

import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

record FilterRenderer(ConditionRenderer conditionRenderer) {

    void render(Filter filter, RenderContext ctx, String baseName, String inputBaseName) {
        conditionRenderer.renderOptimizedFilterIdSelect(filter.condition(), inputBaseName, ctx)
            .ifPresentOrElse(
                optimizedFilterId -> ctx.addCTE(idTable(baseName), optimizedFilterId),
                () -> addFilterIdCTE(ctx, baseName, inputBaseName,
                    conditionRenderer.renderTrue(filter.condition(), inputBaseName, ctx))
            );
        addSelectivePassthroughAttributeCTEs(ctx, baseName, inputBaseName, filter.attributes());
    }

    private void addSelectivePassthroughAttributeCTEs(
        RenderContext ctx,
        String baseName,
        String inputBaseName,
        List<String> attributes
    ) {
        attributes.forEach(attr -> {
            var inputAttrTbl = table(attrTable(inputBaseName, attr));
            var baseIdTbl = table(idTable(baseName));
            var ps = new net.sf.jsqlparser.statement.select.PlainSelect();
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
