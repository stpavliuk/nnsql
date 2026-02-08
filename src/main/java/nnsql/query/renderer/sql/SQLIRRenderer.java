package nnsql.query.renderer.sql;

import nnsql.query.ir.*;
import nnsql.query.renderer.*;

import java.util.stream.Collectors;

public class SQLIRRenderer implements IRRenderer {

    private final ConditionRenderer conditionRenderer;
    private final ProductRenderer   productRenderer;
    private final FilterRenderer    filterRenderer;
    private final GroupRenderer     groupRenderer;
    private final AggFilterRenderer aggFilterRenderer;
    private final ReturnRenderer    returnRenderer;
    private final DuplElimRenderer  duplElimRenderer;

    public SQLIRRenderer() {
        this.conditionRenderer = new ConditionRenderer(this::renderNode);
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
        var lastBaseName = renderNode(ir, ctx);
        var finalSelect = buildFinalSelect(ir, lastBaseName);
        var rootCTEs = findReferencedCTENames(finalSelect, ctx);

        var usedCTEs = ctx.getUsedCTEs(rootCTEs);

        if (usedCTEs.isEmpty()) {
            return finalSelect;
        }

        var ctesStr = usedCTEs.stream()
                              .map(CTE::format)
                              .collect(Collectors.joining(",\n"));

        return Format.withSelect(ctesStr, finalSelect);
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

    private String renderNode(IRNode node, RenderContext ctx) {
        return switch (node) {
            case Product p -> renderWithoutInput("product_", ctx, (c, b) -> {
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

    private String buildFinalSelect(IRNode ir, String baseName) {
        var returnNode = findReturnNode(ir);

        if (returnNode == null || returnNode.selectStar() || returnNode.selectedAttributes().isEmpty()) {
            return "SELECT * FROM " + baseName + "_id;";
        }

        return buildProjectionSelect(baseName, returnNode.selectedAttributes());
    }

    private String buildProjectionSelect(String baseName, java.util.List<Return.AttributeRef> attrs) {
        var selectClause = attrs.stream()
                                .map(attr -> Format.attrCTE(baseName, attr.alias()) + ".v AS " + attr.alias())
                                .collect(Collectors.joining(", "));

        var joins = attrs
            .stream()
            .map(attr -> "JOIN %s ON %s_id.id = %s.id".formatted(
                Format.attrCTE(baseName, attr.alias()),
                baseName,
                Format.attrCTE(baseName, attr.alias())))
            .collect(Collectors.joining("\n"));

        return "SELECT %s\nFROM %s_id\n%s;".formatted(selectClause, baseName, joins);
    }

    private Return findReturnNode(IRNode node) {
        return switch (node) {
            case Return r -> r;
            case DuplElim d -> findReturnNode(d.input());
            default -> null;
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
