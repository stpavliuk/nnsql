package nnsql.query.renderer.sql;

import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.renderer.RenderContext;

import java.util.stream.IntStream;

class ProductRenderer {

    void render(Product product, RenderContext ctx, String baseName) {
        if (product.relations().isEmpty()) {
            throw new IllegalStateException("Product must have at least one relation");
        }

        addAllIdsCTE(ctx, baseName, product);
        addIdCTE(ctx, baseName);
        addAttributeCTEs(ctx, baseName, product);
    }

    private void addAllIdsCTE(RenderContext ctx, String baseName, Product product) {
        var relations = product.relations();

        var selectIdArgs = relations.stream()
            .map(rel -> rel.alias() + "__ID.id")
            .toList();

        var selectIdExprs = IntStream.range(0, relations.size())
            .mapToObj(i -> "       %s AS id%d".formatted(
                relations.get(i).alias() + "__ID.id",
                i + 1
            ))
            .toList();

        var fromTables = relations.stream()
            .map(rel -> switch (rel) {
                case Relation.Table(var tableName, var alias, _) ->
                    tableName + "__ID AS " + alias + "__ID";
                case Relation.Subquery _ ->
                    throw new UnsupportedOperationException("Subqueries not yet fully supported");
            })
            .toList();

        var definition = """
            SELECT %s || '_%d' AS id,
            %s
            FROM %s""".formatted(
                String.join(" || '_' || ", selectIdArgs),
                product.nodeId(),
                String.join(",\n", selectIdExprs),
                String.join(", ", fromTables)
            );

        ctx.addCTE("all_ids_" + baseName, definition);
    }

    private void addIdCTE(RenderContext ctx, String baseName) {
        var definition = "SELECT id FROM all_ids_" + baseName;
        ctx.addCTE(baseName + "_id", definition);
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, Product product) {
        var relations = product.relations();
        IntStream.range(0, relations.size())
            .forEach(relIndex -> {
                var rel = relations.get(relIndex);

                switch (rel) {
                    case Relation.Table(var tableName, var alias, var attrs) ->
                        attrs.forEach(attr -> addAttributeCTE(ctx, baseName, alias, tableName, attr, relIndex + 1));
                    case Relation.Subquery _ ->
                        throw new UnsupportedOperationException("Subqueries not yet fully supported");
                }
            });
    }

    private void addAttributeCTE(RenderContext ctx, String baseName, String alias,
                                 String tableName, String attr, int idIndex) {
        var qualifiedAttr = alias + "_" + attr;

        var definition = """
            SELECT all_ids_%s.id, %s_%s.v
            FROM all_ids_%s, %s_%s
            WHERE all_ids_%s.id%d = %s_%s.id""".formatted(
                baseName, tableName, attr,
                baseName, tableName, attr,
                baseName, idIndex, tableName, attr
            );

        ctx.addCTE(baseName + "_" + qualifiedAttr, definition);
    }
}
