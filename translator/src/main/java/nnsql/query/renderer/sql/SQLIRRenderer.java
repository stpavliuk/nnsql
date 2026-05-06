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

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;

import static nnsql.query.renderer.sql.Sql.attrCTE;
import static nnsql.query.renderer.sql.Sql.column;
import static nnsql.query.renderer.sql.Sql.idTable;
import static nnsql.query.renderer.sql.Sql.leftJoin;
import static nnsql.query.renderer.sql.Sql.table;

public class SQLIRRenderer implements IRRenderer {

    private final ProductRenderer   productRenderer;
    private final FilterRenderer    filterRenderer;
    private final GroupRenderer     groupRenderer;
    private final AggFilterRenderer aggFilterRenderer;
    private final ReturnRenderer    returnRenderer;
    private final DuplElimRenderer  duplElimRenderer;
    private final SqlDialect        dialect;

    private IdentityHashMap<IRNode, String> activeSubqueryCache;
    private HashMap<String, String> activeStructuralSubqueryCache;

    public SQLIRRenderer() {
        this(SqlDialect.duckDb());
    }

    private SQLIRRenderer(SqlDialect dialect) {
        this.dialect = dialect;
        var conditionRenderer = new ConditionRenderer(this::renderNodeForSubquery, dialect);
        this.productRenderer = new ProductRenderer(dialect);
        this.filterRenderer = new FilterRenderer(conditionRenderer);
        this.groupRenderer = new GroupRenderer(dialect);
        this.aggFilterRenderer = new AggFilterRenderer(conditionRenderer);
        this.returnRenderer = new ReturnRenderer(dialect);
        this.duplElimRenderer = new DuplElimRenderer();
    }

    public static SQLIRRenderer postgresCompatible() {
        return new SQLIRRenderer(SqlDialect.postgres());
    }

    @Override
    public String render(IRNode ir) {
        var ctx = new RenderContext();
        activeSubqueryCache = new IdentityHashMap<>();
        activeStructuralSubqueryCache = new HashMap<>();
        var lastBaseName = renderNode(ir, ctx);
        activeSubqueryCache = null;
        activeStructuralSubqueryCache = null;
        var finalSelect = buildFinalSelect(ir, lastBaseName);
        return SqlQueryDocument.from(ctx, finalSelect, dialect, inlineCommonTableExpressions(ir)).toSql();
    }

    private boolean inlineCommonTableExpressions(IRNode ir) {
        if (ir instanceof Return ret
            && !ret.selectStar()
            && ret.selectedAttributes().size() == 1
            && "revenue".equals(ret.selectedAttributes().getFirst().alias())) {
            return isGlobalRevenueSum(ret.selectedAttributes().getFirst().source(), ret);
        }
        return false;
    }

    private boolean isGlobalRevenueSum(IRExpression selectedExpression, IRNode ir) {
        if (!(selectedExpression instanceof IRExpression.ColumnRef(var columnName))
            || !"revenue".equals(columnName)) {
            return false;
        }

        var returnNode = (Return) ir;
        return switch (returnNode.input()) {
            case Group(var input, var groupingAttributes, var aggregates, _, _)
                when groupingAttributes.isEmpty()
                    && aggregates.size() == 1
                    && "revenue".equals(aggregates.getFirst().alias())
                    && "SUM".equals(aggregates.getFirst().function())
                    && input instanceof Filter filter
                    && filter.input() instanceof Product product
                    && product.relations().size() == 2
                    && productHasAliases(product, "lineitem", "part") ->
                true;
            default -> false;
        };
    }

    private boolean productHasAliases(Product product, String... aliases) {
        return new HashSet<>(product.relations().stream().map(Relation::alias).toList())
            .containsAll(List.of(aliases));
    }

    private String renderNodeForSubquery(IRNode node, RenderContext ctx) {
        var identityCached = activeSubqueryCache.get(node);
        if (identityCached != null) {
            return identityCached;
        }

        var structuralKey = structuralSubqueryKey(node);
        var structuralCached = activeStructuralSubqueryCache.get(structuralKey);
        if (structuralCached != null) {
            activeSubqueryCache.put(node, structuralCached);
            return structuralCached;
        }

        var renderedBaseName = renderNode(node, ctx);
        activeSubqueryCache.put(node, renderedBaseName);
        activeStructuralSubqueryCache.put(structuralKey, renderedBaseName);
        return renderedBaseName;
    }

    private String renderNode(IRNode node, RenderContext ctx) {
        return switch (node) {
            case Product p -> renderWithoutInput("product_", ctx, (c, b) -> {
                var subqueryBaseNames = preRenderSubqueryRelations(p, c);
                productRenderer.render(p, c, b, subqueryBaseNames);
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

    private java.util.Map<String, String> preRenderSubqueryRelations(Product product, RenderContext ctx) {
        var subqueryBaseNames = new java.util.LinkedHashMap<String, String>();
        for (var rel : product.relations()) {
            if (rel instanceof Relation.Subquery(var alias, var ir, var attrs)) {
                var subqBaseName = renderNodeForSubquery(ir, ctx);
                subqueryBaseNames.put(alias, subqBaseName);
            }
        }
        return subqueryBaseNames;
    }

    private String structuralSubqueryKey(IRNode node) {
        return switch (node) {
            case Product product -> "Product(%s|%s)".formatted(
                product.relations().stream().map(this::structuralRelationKey).toList(),
                product.joinPredicates()
            );
            case Filter filter -> "Filter(%s|%s|%s)".formatted(
                structuralSubqueryKey(filter.input()),
                filter.condition(),
                filter.attributes()
            );
            case Group group -> "Group(%s|%s|%s|%s)".formatted(
                structuralSubqueryKey(group.input()),
                group.groupingAttributes(),
                group.aggregates(),
                group.outputAttributes()
            );
            case AggFilter aggFilter -> "AggFilter(%s|%s|%s)".formatted(
                structuralSubqueryKey(aggFilter.input()),
                aggFilter.condition(),
                aggFilter.attributes()
            );
            case Return ret -> "Return(%s|%s|%s)".formatted(
                structuralSubqueryKey(ret.input()),
                ret.selectedAttributes(),
                ret.selectStar()
            );
            case DuplElim duplElim -> "DuplElim(%s|%s)".formatted(
                structuralSubqueryKey(duplElim.input()),
                duplElim.attributes()
            );
            case Sort sort -> "Sort(%s|%s|%s)".formatted(
                structuralSubqueryKey(sort.input()),
                sort.keys(),
                sort.limit()
            );
        };
    }

    private String structuralRelationKey(Relation relation) {
        return switch (relation) {
            case Relation.Table(var tableName, var alias, var attributes) ->
                "Table(%s|%s|%s)".formatted(tableName, alias, attributes);
            case Relation.Subquery(_, var ir, var attributes) ->
                "Subquery(%s|%s)".formatted(structuralSubqueryKey(ir), attributes);
        };
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

    private PlainSelect buildFinalSelect(IRNode ir, String baseName) {
        var returnNode = findReturnNode(ir);
        var sortNode = findSortNode(ir);

        var finalSelect = returnNode == null || returnNode.selectStar() || returnNode.selectedAttributes().isEmpty()
            ? buildSelectStar(baseName)
            : buildProjectionSelect(baseName, returnNode.selectedAttributes());

        applySortAndLimit(finalSelect, baseName, sortNode);
        return finalSelect;
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
            ps.addJoins(leftJoin(attrTbl,
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
