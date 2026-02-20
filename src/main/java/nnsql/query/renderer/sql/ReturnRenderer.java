package nnsql.query.renderer.sql;

import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.Return;
import nnsql.query.renderer.RenderContext;

import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

class ReturnRenderer {

    void render(Return returnNode, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName);

        if (!returnNode.selectStar()) {
            addAttributeCTEs(ctx, baseName, inputBaseName, returnNode.selectedAttributes());
        }
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName) {
        var ps = new PlainSelect();
        ps.addSelectItem(column("id"));
        ps.setFromItem(table(idTable(inputBaseName)));

        ctx.addCTE(idTable(baseName), ps.toString());
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                   List<Return.AttributeRef> selectedAttributes) {
        selectedAttributes.forEach(attr -> {
            var ps = new PlainSelect();
            ps.addSelectItem(column("id"));
            ps.addSelectItem(column("v"));
            ps.setFromItem(table(attrTable(inputBaseName, attr.sourceName())));

            ctx.addCTE(attrCTE(baseName, attr.alias()), ps.toString());
        });
    }
}
