package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.*;
import nnsql.query.renderer.*;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static nnsql.query.renderer.sql.Sql.attrCTE;
import static nnsql.query.renderer.sql.Sql.attrTable;
import static nnsql.query.renderer.sql.Sql.column;
import static nnsql.query.renderer.sql.Sql.idTable;
import static nnsql.query.renderer.sql.Sql.join;
import static nnsql.query.renderer.sql.Sql.table;
import static nnsql.query.renderer.sql.Sql.withSelect;

public class SQLIRRenderer implements IRRenderer {

    private final ConditionRenderer conditionRenderer;
    private final ProductRenderer   productRenderer;
    private final FilterRenderer    filterRenderer;
    private final GroupRenderer     groupRenderer;
    private final AggFilterRenderer aggFilterRenderer;
    private final ReturnRenderer    returnRenderer;
    private final DuplElimRenderer  duplElimRenderer;

    private IdentityHashMap<IRNode, String> activeSubqueryCache;

    public SQLIRRenderer() {
        this.conditionRenderer = new ConditionRenderer(this::renderNodeForSubquery);
        this.productRenderer = new ProductRenderer();
        this.filterRenderer = new FilterRenderer(conditionRenderer);
        this.groupRenderer = new GroupRenderer();
        this.aggFilterRenderer = new AggFilterRenderer(conditionRenderer);
        this.returnRenderer = new ReturnRenderer();
        this.duplElimRenderer = new DuplElimRenderer();
    }

    @Override
    public String render(IRNode ir) {
        var ctx = new RenderContext();
        activeSubqueryCache = new IdentityHashMap<>();
        var lastBaseName = renderNode(ir, ctx);
        activeSubqueryCache = null;
        var finalSelect = buildFinalSelect(ir, lastBaseName);
        var rootCTEs = findReferencedCTENames(finalSelect, ctx);

        var usedCTEs = ctx.getUsedCTEs(rootCTEs);

        if (usedCTEs.isEmpty()) {
            return finalSelect;
        }

        var ctesStr = usedCTEs.stream()
                              .map(CTE::format)
                              .collect(Collectors.joining(",\n"));

        return withSelect(ctesStr, finalSelect);
    }

    private java.util.Set<String> findReferencedCTENames(String finalSelect, RenderContext ctx) {
        var allCTENames = ctx.getCTEs().stream()
                             .map(CTE::name)
                             .collect(Collectors.toSet());

        var referenced = new java.util.HashSet<String>();
        for (String cteName : allCTENames) {
            if (finalSelect.contains(cteName)) {
                referenced.add(cteName);
            }
        }
        return referenced;
    }

    private String renderNodeForSubquery(IRNode node, RenderContext ctx) {
        return renderNode(node, ctx);
    }

    private String renderNode(IRNode node, RenderContext ctx) {
        return switch (node) {
            case Product p -> renderWithoutInput("product_", ctx, (c, b) -> {
                preRenderSubqueryRelations(p, c);
                productRenderer.render(p, c, b);
                return b;
            });

            case Filter f -> renderWithInput("filter_", f.input(), ctx,
                (c, b, i) -> filterRenderer.render(f, c, b, i));

            case Group g -> renderWithInput("group_", g.input(), ctx,
                (c, b, i) -> groupRenderer.render(g, c, b, i));

            case AggFilter af -> renderWithInput("aggfilter_", af.input(), ctx,
                (c, b, i) -> aggFilterRenderer.render(af, c, b, i));

            case Return r -> renderWithInput("return_", r.input(), ctx,
                (c, b, i) -> returnRenderer.render(r, c, b, i));

            case DuplElim d -> renderWithInput("duplelim_", d.input(), ctx,
                (c, b, i) -> duplElimRenderer.render(d, c, b, i));

            case Sort s -> renderNode(s.input(), ctx);
        };
    }

    private void preRenderSubqueryRelations(Product product, RenderContext ctx) {
        for (var rel : product.relations()) {
            if (rel instanceof Relation.Subquery(var alias, var ir, var attrs)) {
                var subqBaseName = activeSubqueryCache.computeIfAbsent(
                    ir, k -> renderNode(k, ctx));
                ctx.addCTE(alias + "__ID", "SELECT id FROM " + idTable(subqBaseName));
                for (var attr : attrs) {
                    ctx.addCTE(attrTable(alias, attr),
                        "SELECT id, v FROM " + attrCTE(subqBaseName, attr));
                }
            }
        }
    }

    private String renderWithoutInput(String prefix, RenderContext ctx,
                                      RendererFunction renderer) {
        var baseName = ctx.nextName(prefix);
        return renderer.render(ctx, baseName);
    }

    private String renderWithInput(String prefix, IRNode input, RenderContext ctx,
                                   RendererWithInputFunction renderer) {
        var inputBaseName = renderNode(input, ctx);
        var baseName = ctx.nextName(prefix);
        renderer.render(ctx, baseName, inputBaseName);
        return baseName;
    }

    private String buildFinalSelect(IRNode ir, String baseName) {
        var returnNode = findReturnNode(ir);
        var sortNode = findSortNode(ir);

        var finalSelect = returnNode == null || returnNode.selectStar() || returnNode.selectedAttributes().isEmpty()
            ? buildSelectStar(baseName)
            : buildProjectionSelect(baseName, returnNode.selectedAttributes());

        applySortAndLimit(finalSelect, baseName, sortNode);
        return finalSelect + ";";
    }

    private PlainSelect buildSelectStar(String baseName) {
        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(table(idTable(baseName)));
        return ps;
    }

    private PlainSelect buildProjectionSelect(String baseName, List<Return.AttributeRef> attrs) {
        var baseIdTbl = table(idTable(baseName));

        var ps = new PlainSelect();
        for (var attr : attrs) {
            var attrTbl = table(attrCTE(baseName, attr.alias()));
            ps.addSelectItem(column(attrTbl, "v"), new Alias(attr.alias()));
        }
        ps.setFromItem(baseIdTbl);
        for (var attr : attrs) {
            var attrTbl = table(attrCTE(baseName, attr.alias()));
            ps.addJoins(join(attrTbl,
                new EqualsTo(column(baseIdTbl, "id"), column(attrTbl, "id"))));
        }

        return ps;
    }

    private void applySortAndLimit(PlainSelect ps, String baseName, Sort sortNode) {
        if (sortNode == null) {
            return;
        }

        if (!sortNode.keys().isEmpty()) {
            var orderByElements = sortNode.keys().stream()
                .map(key -> {
                    var element = new OrderByElement();
                    element.setExpression(column(attrCTE(baseName, key.attribute()), "v"));
                    element.setAsc(!key.descending());
                    element.setAscDescPresent(true);
                    return element;
                })
                .toList();
            ps.setOrderByElements(orderByElements);
        }

        if (sortNode.limit() != null) {
            var limit = new Limit();
            limit.setRowCount(new LongValue(sortNode.limit()));
            ps.setLimit(limit);
        }
    }

    private Return findReturnNode(IRNode node) {
        return switch (node) {
            case Return _, Sort _, DuplElim _ -> IRNodeTraversal.findReturnNode(node);
            default -> null;
        };
    }

    private Sort findSortNode(IRNode node) {
        return switch (node) {
            case Sort s -> s;
            case DuplElim d -> findSortNode(d.input());
            case Return r -> findSortNode(r.input());
            case AggFilter af -> findSortNode(af.input());
            case Group g -> findSortNode(g.input());
            case Filter f -> findSortNode(f.input());
            case Product _ -> null;
        };
    }

    @FunctionalInterface
    private interface RendererFunction {
        String render(RenderContext ctx, String baseName);
    }

    @FunctionalInterface
    private interface RendererWithInputFunction {
        void render(RenderContext ctx, String baseName, String inputBaseName);
    }
}
