package nnsql.query.renderer.sql;

import nnsql.query.ir.Filter;
import nnsql.query.renderer.RenderContext;

import java.util.List;

record FilterRenderer(ConditionRenderer conditionRenderer) {

    void render(Filter filter, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName, filter);
        addAttributeCTEs(ctx, baseName, inputBaseName, filter.attributes());
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName, Filter filter) {
        var definition = """
            SELECT %s_id.id
            FROM %s_id
            WHERE %s""".formatted(
                inputBaseName,
                inputBaseName,
                conditionRenderer.renderTrue(filter.condition(), inputBaseName, ctx)
            );

        ctx.addCTE(baseName + "_id", definition);
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName, List<String> attributes) {
        attributes.forEach(attr -> {
            var definition = """
                SELECT %s_%s.*
                FROM %s_%s JOIN %s_id ON %s_id.id = %s_%s.id""".formatted(
                inputBaseName, attr,
                inputBaseName, attr, baseName, baseName, inputBaseName, attr
            );

            ctx.addCTE(baseName + "_" + attr, definition);
        });
    }
}
