package nnsql.query.renderer.sql;

import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.Filter;
import nnsql.query.renderer.RenderContext;

import static nnsql.query.renderer.sql.Sql.*;

record FilterRenderer(ConditionRenderer conditionRenderer) {

    void render(Filter filter, RenderContext ctx, String baseName, String inputBaseName) {
        conditionRenderer.renderOptimizedFilterIdSelect(filter.condition(), inputBaseName, ctx)
            .map(PlainSelect::toString)
            .ifPresentOrElse(
                optimizedFilterId -> ctx.addCTE(idTable(baseName), optimizedFilterId),
                () -> addFilterIdCTE(ctx, baseName, inputBaseName,
                    conditionRenderer.renderTrue(filter.condition(), inputBaseName, ctx))
            );
        addPassthroughAttributeCTEs(ctx, baseName, inputBaseName, filter.attributes(), Sql::attrTable);
    }
}
