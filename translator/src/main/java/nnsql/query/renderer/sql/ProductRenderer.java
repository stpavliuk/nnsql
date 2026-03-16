package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.Product;
import nnsql.query.ir.Relation;
import nnsql.query.optim.JoinPredicate;
import nnsql.query.renderer.RenderContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
        if (product.joinPredicates().isEmpty()) {
            addCartesianProductCTE(ctx, baseName, product);
        } else {
            addJoinProductCTE(ctx, baseName, product);
        }
    }

    private void addCartesianProductCTE(RenderContext ctx, String baseName, Product product) {
        var relations = product.relations();

        var ps = buildSelectItems(product);

        Table firstFrom = idTableFor(relations.getFirst());
        ps.setFromItem(firstFrom);

        for (int i = 1; i < relations.size(); i++) {
            ps.addJoins(simpleJoin(idTableFor(relations.get(i))));
        }

        ctx.addCTE("all_ids_" + baseName, ps.toString());
    }

    private void addJoinProductCTE(RenderContext ctx, String baseName, Product product) {
        var relations = product.relations();
        var predicates = new ArrayList<>(product.joinPredicates());

        var joined = new LinkedHashSet<Integer>();
        joined.add(0);

        record JoinStep(int newRelIndex, int existingRelIndex,
                        String newAttr, String existingAttr) {
        }
        var steps = new ArrayList<JoinStep>();
        var crossJoined = new ArrayList<Integer>();

        while (joined.size() < relations.size()) {
            boolean found = false;
            for (var iter = predicates.iterator(); iter.hasNext(); ) {
                var jp = iter.next();
                JoinStep step = null;

                if (!joined.contains(jp.leftRelIndex()) && joined.contains(jp.rightRelIndex())) {
                    step = new JoinStep(
                        jp.leftRelIndex(), jp.rightRelIndex(),
                        jp.leftAttr(), jp.rightAttr());
                } else if (joined.contains(jp.leftRelIndex()) && !joined.contains(jp.rightRelIndex())) {
                    step = new JoinStep(
                        jp.rightRelIndex(), jp.leftRelIndex(),
                        jp.rightAttr(), jp.leftAttr());
                }

                if (step != null) {
                    steps.add(step);
                    joined.add(step.newRelIndex());
                    iter.remove();
                    found = true;
                    break;
                }
            }

            if (!found) {
                for (int i = 0; i < relations.size(); i++) {
                    if (!joined.contains(i)) {
                        crossJoined.add(i);
                        joined.add(i);
                        break;
                    }
                }
            }
        }

        var ps = buildSelectItems(product);
        ps.setFromItem(idTableFor(relations.getFirst()));

        var joins = new ArrayList<Join>();
        for (int si = 0; si < steps.size(); si++) {
            var step = steps.get(si);
            var existingRel = relations.get(step.existingRelIndex());
            var newRel = relations.get(step.newRelIndex());

            var existingAttrTbl = tableAs(
                tableNameFor(existingRel) + "_" + step.existingAttr(),
                "_jp" + si + "l");
            var newAttrTbl = tableAs(
                tableNameFor(newRel) + "_" + step.newAttr(),
                "_jp" + si + "r");
            var newIdTbl = idTableFor(newRel);

            joins.add(join(existingAttrTbl,
                new EqualsTo(
                    column(existingRel.alias() + "__ID", "id"),
                    column("_jp" + si + "l", "id"))));

            joins.add(join(newAttrTbl,
                new EqualsTo(
                    column("_jp" + si + "l", "v"),
                    column("_jp" + si + "r", "v"))));

            joins.add(join(newIdTbl,
                new EqualsTo(
                    column("_jp" + si + "r", "id"),
                    column(newRel.alias() + "__ID", "id"))));
        }

        for (var crossIdx : crossJoined) {
            joins.add(simpleJoin(idTableFor(relations.get(crossIdx))));
        }

        // Extra predicates where both relations are already joined via other predicates
        var extraConditions = new ArrayList<Expression>();
        for (int pi = 0; pi < predicates.size(); pi++) {
            var jp = predicates.get(pi);
            var leftRel = relations.get(jp.leftRelIndex());
            var rightRel = relations.get(jp.rightRelIndex());

            var leftAttrTbl = tableAs(
                tableNameFor(leftRel) + "_" + jp.leftAttr(),
                "_jpx" + pi + "l");
            var rightAttrTbl = tableAs(
                tableNameFor(rightRel) + "_" + jp.rightAttr(),
                "_jpx" + pi + "r");

            joins.add(join(leftAttrTbl,
                new EqualsTo(
                    column(leftRel.alias() + "__ID", "id"),
                    column("_jpx" + pi + "l", "id"))));
            joins.add(join(rightAttrTbl,
                new EqualsTo(
                    column(rightRel.alias() + "__ID", "id"),
                    column("_jpx" + pi + "r", "id"))));

            extraConditions.add(new EqualsTo(
                column("_jpx" + pi + "l", "v"),
                column("_jpx" + pi + "r", "v")));
        }

        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }

        if (!extraConditions.isEmpty()) {
            ps.setWhere(andAll(extraConditions));
        }

        ctx.addCTE("all_ids_" + baseName, ps.toString());
    }

    private PlainSelect buildSelectItems(Product product) {
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

        return ps;
    }

    private static Table idTableFor(Relation rel) {
        return switch (rel) {
            case Relation.Table(var tableName, var alias, _) ->
                tableAs(tableName + "__ID", alias + "__ID");
            case Relation.Subquery(var alias, _, _) ->
                tableAs(alias + "__ID", alias + "__ID");
        };
    }

    private static String tableNameFor(Relation rel) {
        return switch (rel) {
            case Relation.Table(var tableName, _, _) -> tableName;
            case Relation.Subquery(var alias, _, _) -> alias;
        };
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
