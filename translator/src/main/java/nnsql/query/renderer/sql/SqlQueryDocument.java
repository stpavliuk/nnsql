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
    private final SqlDialect dialect;
    private final boolean forceInlineCtes;

    private SqlQueryDocument(
        PlainSelect finalSelect,
        List<CTE> ctes,
        SqlDialect dialect,
        boolean forceInlineCtes
    ) {
        this.finalSelect = finalSelect;
        this.ctes = ctes;
        this.dialect = dialect;
        this.forceInlineCtes = forceInlineCtes;
    }

    static SqlQueryDocument from(
        RenderContext ctx,
        PlainSelect finalSelect,
        SqlDialect dialect,
        boolean forceInlineCtes
    ) {
        var rootCTEs = RenderContext.dependenciesOf(finalSelect);
        return new SqlQueryDocument(finalSelect, ctx.getUsedCTEs(rootCTEs), dialect, forceInlineCtes);
    }

    String toSql() {
        if (!ctes.isEmpty()) {
            finalSelect.setWithItemsList(ctes.stream()
                .map(this::toWithItem)
                .toList());
        }
        return finalSelect + ";";
    }

    private WithItem<?> toWithItem(CTE cte) {
        var select = new ParenthesedSelect();
        select.setSelect(cte.definition());

        var withItem = new WithItem<ParenthesedSelect>();
        withItem.setAlias(new Alias(cte.name(), false));
        withItem.setSelect(select);
        withItem.setMaterialized(!forceInlineCtes && dialect.materializeCommonTableExpressions());
        return withItem;
    }
}
