package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.renderer.RenderContext;

import java.util.stream.IntStream;

import static nnsql.query.renderer.sql.Sql.*;

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

        var idRefs = relations.stream()
            .map(rel -> (Expression) column(rel.alias() + "__ID", "id"))
            .toList();

        var idConcat = concatSep(idRefs, "_");
        Expression compositeId = new net.sf.jsqlparser.expression.operators.arithmetic.Concat(
            idConcat, new StringValue("_" + product.nodeId()));

        var ps = new PlainSelect();
        ps.addSelectItem(compositeId, new Alias("id", true));

        for (int i = 0; i < relations.size(); i++) {
            ps.addSelectItem(
                column(relations.get(i).alias() + "__ID", "id"),
                new Alias("id" + (i + 1), true));
        }

        Table firstFrom = switch (relations.getFirst()) {
            case Relation.Table(var tableName, var alias, _) ->
                tableAs(tableName + "__ID", alias + "__ID");
            case Relation.Subquery(var alias, _, _) ->
                tableAs(alias + "__ID", alias + "__ID");
        };
        ps.setFromItem(firstFrom);

        for (int i = 1; i < relations.size(); i++) {
            Table tbl = switch (relations.get(i)) {
                case Relation.Table(var tableName, var alias, _) ->
                    tableAs(tableName + "__ID", alias + "__ID");
                case Relation.Subquery(var alias, _, _) ->
                    tableAs(alias + "__ID", alias + "__ID");
            };
            ps.addJoins(simpleJoin(tbl));
        }

        ctx.addCTE("all_ids_" + baseName, ps.toString());
    }

    private void addIdCTE(RenderContext ctx, String baseName) {
        var ps = new PlainSelect();
        ps.addSelectItem(column("id"));
        ps.setFromItem(table("all_ids_" + baseName));

        ctx.addCTE(idTable(baseName), ps.toString());
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, Product product) {
        var relations = product.relations();
        IntStream.range(0, relations.size())
            .forEach(relIndex -> {
                var rel = relations.get(relIndex);

                switch (rel) {
                    case Relation.Table(var tableName, var alias, var attrs) ->
                        attrs.forEach(attr -> addAttributeCTE(ctx, baseName, alias, tableName, attr, relIndex + 1));
                    case Relation.Subquery(var alias, _, var attrs) ->
                        attrs.forEach(attr -> addAttributeCTE(ctx, baseName, alias, alias, attr, relIndex + 1));
                }
            });
    }

    private void addAttributeCTE(RenderContext ctx, String baseName, String alias,
                                 String tableName, String attr, int idIndex) {
        var qualifiedAttr = alias + "_" + attr;
        var allIdsTbl = table("all_ids_" + baseName);
        var attrTbl = table(attrTable(tableName, attr));

        var ps = new PlainSelect();
        ps.addSelectItem(column(allIdsTbl, "id"));
        ps.addSelectItem(column(attrTbl, "v"));
        ps.setFromItem(allIdsTbl);
        ps.addJoins(simpleJoin(attrTbl));
        ps.setWhere(new EqualsTo(
            column(allIdsTbl, "id" + idIndex),
            column(attrTbl, "id")));

        ctx.addCTE(attrTable(baseName, qualifiedAttr), ps.toString());
    }
}
