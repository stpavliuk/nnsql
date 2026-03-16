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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.IntStream;

import static nnsql.query.renderer.sql.Sql.*;

class ProductRenderer {

    void render(Product product, RenderContext ctx, String baseName) {
        if (product.relations().isEmpty()) {
            throw new IllegalStateException("Product must have at least one relation");
        }

        if (product.relations().size() == 1) {
            addUnaryProductCTEs(ctx, baseName, product.relations().getFirst());
            return;
        }

        addAllIdsCTE(ctx, baseName, product);
        addIdCTE(ctx, baseName);
        addAttributeCTEs(ctx, baseName, product);
    }

    private void addUnaryProductCTEs(RenderContext ctx, String baseName, Relation relation) {
        var idSource = idTableFor(relation);

        var idSelect = new PlainSelect();
        idSelect.addSelectItem(column(idSource, "id"));
        idSelect.setFromItem(idSource);
        ctx.addCTE(idTable(baseName), idSelect.toString());

        switch (relation) {
            case Relation.Table(var tableName, var alias, var attrs) ->
                attrs.forEach(attr -> addUnaryAttributeCTE(ctx, baseName, alias, tableName, attr));
            case Relation.Subquery(var alias, _, var attrs) ->
                attrs.forEach(attr -> addUnaryAttributeCTE(ctx, baseName, alias, alias, attr));
        }
    }

    private void addUnaryAttributeCTE(
        RenderContext ctx,
        String baseName,
        String alias,
        String tableName,
        String attr
    ) {
        var sourceAttrTbl = table(attrTable(tableName, attr));
        var ps = new PlainSelect();
        ps.addSelectItem(column(sourceAttrTbl, "id"));
        ps.addSelectItem(column(sourceAttrTbl, "v"));
        ps.setFromItem(sourceAttrTbl);
        ctx.addCTE(attrTable(baseName, alias + "_" + attr), ps.toString());
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
        var joinedAttrAliases = new LinkedHashMap<RelationAttr, String>();
        int[] aliasCounter = {0};
        for (int si = 0; si < steps.size(); si++) {
            var step = steps.get(si);
            var existingRel = relations.get(step.existingRelIndex());
            var newRel = relations.get(step.newRelIndex());

            var existingAttrAlias = ensureJoinedAttributeAlias(
                joins,
                joinedAttrAliases,
                new RelationAttr(step.existingRelIndex(), step.existingAttr()),
                relations,
                aliasCounter
            );
            var newAttrAlias = joinNewRelationAttribute(
                joins,
                joinedAttrAliases,
                new RelationAttr(step.newRelIndex(), step.newAttr()),
                existingAttrAlias,
                relations,
                aliasCounter
            );
            var newIdTbl = idTableFor(newRel);

            joins.add(join(newIdTbl,
                new EqualsTo(
                    column(newAttrAlias, "id"),
                    column(newRel.alias() + "__ID", "id"))));
        }

        for (var crossIdx : crossJoined) {
            joins.add(simpleJoin(idTableFor(relations.get(crossIdx))));
        }

        // Extra predicates where both relations are already joined via other predicates
        var extraConditions = new ArrayList<Expression>();
        for (int pi = 0; pi < predicates.size(); pi++) {
            var jp = predicates.get(pi);
            var leftAttrAlias = ensureJoinedAttributeAlias(
                joins,
                joinedAttrAliases,
                new RelationAttr(jp.leftRelIndex(), jp.leftAttr()),
                relations,
                aliasCounter
            );
            var rightAttrAlias = ensureJoinedAttributeAlias(
                joins,
                joinedAttrAliases,
                new RelationAttr(jp.rightRelIndex(), jp.rightAttr()),
                relations,
                aliasCounter
            );

            extraConditions.add(new EqualsTo(
                column(leftAttrAlias, "v"),
                column(rightAttrAlias, "v")));
        }

        if (!joins.isEmpty()) {
            ps.setJoins(joins);
        }

        if (!extraConditions.isEmpty()) {
            ps.setWhere(andAll(extraConditions));
        }

        ctx.addCTE("all_ids_" + baseName, ps.toString());
    }

    private String ensureJoinedAttributeAlias(
        ArrayList<Join> joins,
        Map<RelationAttr, String> joinedAttrAliases,
        RelationAttr relationAttr,
        java.util.List<Relation> relations,
        int[] aliasCounter
    ) {
        var existing = joinedAttrAliases.get(relationAttr);
        if (existing != null) {
            return existing;
        }

        var relation = relations.get(relationAttr.relIndex());
        var alias = "_jp" + aliasCounter[0]++;
        joins.add(join(
            tableAs(tableNameFor(relation) + "_" + relationAttr.attr(), alias),
            new EqualsTo(
                column(relation.alias() + "__ID", "id"),
                column(alias, "id")
            )
        ));
        joinedAttrAliases.put(relationAttr, alias);
        return alias;
    }

    private String joinNewRelationAttribute(
        ArrayList<Join> joins,
        Map<RelationAttr, String> joinedAttrAliases,
        RelationAttr relationAttr,
        String existingAttrAlias,
        java.util.List<Relation> relations,
        int[] aliasCounter
    ) {
        var existing = joinedAttrAliases.get(relationAttr);
        if (existing != null) {
            return existing;
        }

        var relation = relations.get(relationAttr.relIndex());
        var alias = "_jp" + aliasCounter[0]++;
        joins.add(join(
            tableAs(tableNameFor(relation) + "_" + relationAttr.attr(), alias),
            new EqualsTo(
                column(existingAttrAlias, "v"),
                column(alias, "v")
            )
        ));
        joinedAttrAliases.put(relationAttr, alias);
        return alias;
    }

    private record RelationAttr(int relIndex, String attr) {
    }

    private PlainSelect buildSelectItems(Product product) {
        var relations = product.relations();

        var hashArgs = new ArrayList<Expression>();
        relations.forEach(rel -> hashArgs.add(column(rel.alias() + "__ID", "id")));
        hashArgs.add(new LongValue(product.nodeId()));

        Expression compositeId = fn("hash", hashArgs.toArray(Expression[]::new));

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
