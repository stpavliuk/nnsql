package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import nnsql.query.renderer.CTE;
import nnsql.query.renderer.RenderContext;

import java.util.List;

final class SqlQueryDocument {
    private final PlainSelect finalSelect;
    private final List<CTE> ctes;

    private SqlQueryDocument(PlainSelect finalSelect, List<CTE> ctes) {
        this.finalSelect = finalSelect;
        this.ctes = ctes;
    }

    static SqlQueryDocument from(RenderContext ctx, PlainSelect finalSelect) {
        var rootCTEs = RenderContext.dependenciesOf(finalSelect);
        return new SqlQueryDocument(finalSelect, ctx.getUsedCTEs(rootCTEs));
    }

    String toSql() {
        if (!ctes.isEmpty()) {
            finalSelect.setWithItemsList(ctes.stream()
                .map(SqlQueryDocument::toWithItem)
                .toList());
        }
        return finalSelect + ";";
    }

    private static WithItem<?> toWithItem(CTE cte) {
        var select = new ParenthesedSelect();
        select.setSelect(cte.definition());

        var withItem = new WithItem<ParenthesedSelect>();
        withItem.setAlias(new Alias(cte.name(), false));
        withItem.setSelect(select);
        return withItem;
    }
}
