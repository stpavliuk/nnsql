package nnsql.query.renderer.sql;

import nnsql.query.ir.Filter;
import nnsql.query.renderer.RenderContext;

import static nnsql.query.renderer.sql.Sql.*;

record FilterRenderer(ConditionRenderer conditionRenderer) {

    void render(Filter filter, RenderContext ctx, String baseName, String inputBaseName) {
        addFilterIdCTE(ctx, baseName, inputBaseName,
            conditionRenderer.renderTrue(filter.condition(), inputBaseName, ctx));
        addPassthroughAttributeCTEs(ctx, baseName, inputBaseName, filter.attributes(), Sql::attrTable);
    }
}
