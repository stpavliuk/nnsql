package nnsql.query.renderer.sql;

import nnsql.query.ir.Return;
import nnsql.query.renderer.RenderContext;

class ReturnRenderer {

    void render(Return returnNode, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName);

        if (!returnNode.selectStar()) {
            addAttributeCTEs(ctx, baseName, inputBaseName, returnNode.selectedAttributes());
        }
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName) {
        var definition = "SELECT id FROM " + inputBaseName + "_id";
        ctx.addCTE(baseName + "_id", definition);
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                  java.util.List<Return.AttributeRef> selectedAttributes) {
        selectedAttributes.forEach(attr -> {
            var definition = """
                SELECT id, v FROM %s_%s""".formatted(
                    inputBaseName, attr.sourceName()
                );

            ctx.addCTE(baseName + "_attr_" + attr.alias(), definition);
        });
    }
}
