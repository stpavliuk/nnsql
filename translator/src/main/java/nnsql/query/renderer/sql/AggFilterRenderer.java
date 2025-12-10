package nnsql.query.renderer.sql;

import nnsql.query.ir.AggFilter;
import nnsql.query.renderer.RenderContext;

import java.util.List;

record AggFilterRenderer(ConditionRenderer conditionRenderer) {

    void render(AggFilter aggFilter, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName, aggFilter);
        addAttributeCTEs(ctx, baseName, inputBaseName, aggFilter.attributes());
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName, AggFilter aggFilter) {
        var definition = """
            SELECT %s_id.id
            FROM %s_id
            WHERE %s""".formatted(
            inputBaseName,
            inputBaseName,
            conditionRenderer.renderTrue(aggFilter.condition(), inputBaseName, ctx)
        );

        ctx.addCTE(baseName + "_id", definition);
    }

    private void addAttributeCTEs(
        RenderContext ctx,
        String baseName,
        String inputBaseName,
        List<String> attributes
    ) {
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
