package nnsql.query.renderer.sql;

import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.AggFilter;
import nnsql.query.renderer.RenderContext;

import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

record AggFilterRenderer(ConditionRenderer conditionRenderer) {

    void render(AggFilter aggFilter, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName, aggFilter);
        addAttributeCTEs(ctx, baseName, inputBaseName, aggFilter.attributes());
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName, AggFilter aggFilter) {
        var idTbl = table(idTable(inputBaseName));

        var ps = new PlainSelect();
        ps.addSelectItem(column(idTbl, "id"));
        ps.setFromItem(idTbl);
        ps.setWhere(conditionRenderer.renderTrue(aggFilter.condition(), inputBaseName, ctx));

        ctx.addCTE(idTable(baseName), ps.toString());
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                   List<String> attributes) {
        attributes.forEach(attr -> {
            var inputAttrTbl = table(attrTable(inputBaseName, attr));
            var baseIdTbl = table(idTable(baseName));

            var ps = new PlainSelect();
            ps.addSelectItem(new AllTableColumns(inputAttrTbl));
            ps.setFromItem(inputAttrTbl);
            ps.addJoins(join(baseIdTbl,
                new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                    column(baseIdTbl, "id"), column(inputAttrTbl, "id"))));

            ctx.addCTE(attrTable(baseName, attr), ps.toString());
        });
    }
}
