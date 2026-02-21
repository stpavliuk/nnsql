package nnsql.query.renderer.sql;

import nnsql.query.ir.AggFilter;
import nnsql.query.renderer.RenderContext;

import static nnsql.query.renderer.sql.Sql.*;

record AggFilterRenderer(ConditionRenderer conditionRenderer) {

    void render(AggFilter aggFilter, RenderContext ctx, String baseName, String inputBaseName) {
        addFilterIdCTE(ctx, baseName, inputBaseName,
            conditionRenderer.renderTrue(aggFilter.condition(), inputBaseName, ctx));
        addPassthroughAttributeCTEs(ctx, baseName, inputBaseName, aggFilter.attributes(), Sql::attrTable);
    }
}
