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

    void render(
        Product product,
        RenderContext ctx,
        String baseName,
        Map<String, String> subqueryBaseNames
    ) {
        if (product.relations().isEmpty()) {
            throw new IllegalStateException("Product must have at least one relation");
        }

        if (product.relations().size() == 1) {
            addUnaryProductCTEs(ctx, baseName, product.relations().getFirst(), subqueryBaseNames);
            return;
        }

        addAllIdsCTE(ctx, baseName, product, subqueryBaseNames);
        addIdCTE(ctx, baseName);
        addAttributeCTEs(ctx, baseName, product, subqueryBaseNames);
    }

    private void addUnaryProductCTEs(
        RenderContext ctx,
        String baseName,
        Relation relation,
        Map<String, String> subqueryBaseNames
    ) {
        var idSource = idTableFor(relation, subqueryBaseNames);

        var idSelect = new PlainSelect();
        idSelect.addSelectItem(column(idSource, "id"));
        idSelect.setFromItem(idSource);
        ctx.addCTE(idTable(baseName), idSelect.toString());

        switch (relation) {
            case Relation.Table(var tableName, var alias, var attrs) ->
                attrs.forEach(attr -> addUnaryAttributeCTE(
                    ctx,
                    baseName,
                    alias + "_" + attr,
                    sourceAttrTableFor(relation, attr, subqueryBaseNames)
                ));
            case Relation.Subquery(var alias, _, var attrs) ->
                attrs.forEach(attr -> addUnaryAttributeCTE(
                    ctx,
                    baseName,
                    alias + "_" + attr,
                    sourceAttrTableFor(relation, attr, subqueryBaseNames)
                ));
        }
    }

    private void addUnaryAttributeCTE(
        RenderContext ctx,
        String baseName,
        String qualifiedAttr,
        Table sourceAttrTbl
    ) {
        var ps = new PlainSelect();
        ps.addSelectItem(column(sourceAttrTbl, "id"));
        ps.addSelectItem(column(sourceAttrTbl, "v"));
        ps.setFromItem(sourceAttrTbl);
        ctx.addCTE(attrTable(baseName, qualifiedAttr), ps.toString());
    }

    private void addAllIdsCTE(
        RenderContext ctx,
        String baseName,
        Product product,
        Map<String, String> subqueryBaseNames
    ) {
        if (product.joinPredicates().isEmpty()) {
            addCartesianProductCTE(ctx, baseName, product, subqueryBaseNames);
        } else {
            addJoinProductCTE(ctx, baseName, product, subqueryBaseNames);
        }
    }

    private void addCartesianProductCTE(
        RenderContext ctx,
        String baseName,
        Product product,
        Map<String, String> subqueryBaseNames
    ) {
        var relations = product.relations();

        var ps = buildSelectItems(product);

        Table firstFrom = idTableFor(relations.getFirst(), subqueryBaseNames);
        ps.setFromItem(firstFrom);

        for (int i = 1; i < relations.size(); i++) {
            ps.addJoins(simpleJoin(idTableFor(relations.get(i), subqueryBaseNames)));
        }

        ctx.addCTE("all_ids_" + baseName, ps.toString());
    }

    private void addJoinProductCTE(
        RenderContext ctx,
        String baseName,
        Product product,
        Map<String, String> subqueryBaseNames
    ) {
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
        ps.setFromItem(idTableFor(relations.getFirst(), subqueryBaseNames));

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
                subqueryBaseNames,
                aliasCounter
            );
            var newAttrAlias = joinNewRelationAttribute(
                joins,
                joinedAttrAliases,
                new RelationAttr(step.newRelIndex(), step.newAttr()),
                existingAttrAlias,
                relations,
                subqueryBaseNames,
                aliasCounter
            );
            var newIdTbl = idTableFor(newRel, subqueryBaseNames);

            joins.add(join(newIdTbl,
                new EqualsTo(
                    column(newAttrAlias, "id"),
                    column(newRel.alias() + "__ID", "id"))));
        }

        for (var crossIdx : crossJoined) {
            joins.add(simpleJoin(idTableFor(relations.get(crossIdx), subqueryBaseNames)));
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
                subqueryBaseNames,
                aliasCounter
            );
            var rightAttrAlias = ensureJoinedAttributeAlias(
                joins,
                joinedAttrAliases,
                new RelationAttr(jp.rightRelIndex(), jp.rightAttr()),
                relations,
                subqueryBaseNames,
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
        Map<String, String> subqueryBaseNames,
        int[] aliasCounter
    ) {
        var existing = joinedAttrAliases.get(relationAttr);
        if (existing != null) {
            return existing;
        }

        var relation = relations.get(relationAttr.relIndex());
        var alias = "_jp" + aliasCounter[0]++;
        joins.add(join(
            tableAs(sourceAttrTableNameFor(relation, relationAttr.attr(), subqueryBaseNames), alias),
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
        Map<String, String> subqueryBaseNames,
        int[] aliasCounter
    ) {
        var existing = joinedAttrAliases.get(relationAttr);
        if (existing != null) {
            return existing;
        }

        var relation = relations.get(relationAttr.relIndex());
        var alias = "_jp" + aliasCounter[0]++;
        joins.add(join(
            tableAs(sourceAttrTableNameFor(relation, relationAttr.attr(), subqueryBaseNames), alias),
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

    private static Table idTableFor(Relation rel, Map<String, String> subqueryBaseNames) {
        return switch (rel) {
            case Relation.Table(var tableName, var alias, _) ->
                tableAs(tableName + "__ID", alias + "__ID");
            case Relation.Subquery(var alias, _, _) ->
                tableAs(idTable(requireSubqueryBaseName(alias, subqueryBaseNames)), alias + "__ID");
        };
    }

    private static Table sourceAttrTableFor(
        Relation rel,
        String attr,
        Map<String, String> subqueryBaseNames
    ) {
        return table(sourceAttrTableNameFor(rel, attr, subqueryBaseNames));
    }

    private static String sourceAttrTableNameFor(
        Relation rel,
        String attr,
        Map<String, String> subqueryBaseNames
    ) {
        return switch (rel) {
            case Relation.Table(var tableName, _, _) -> attrTable(tableName, attr);
            case Relation.Subquery(var alias, _, _) -> attrCTE(requireSubqueryBaseName(alias, subqueryBaseNames), attr);
        };
    }

    private static String requireSubqueryBaseName(String alias, Map<String, String> subqueryBaseNames) {
        var subqueryBaseName = subqueryBaseNames.get(alias);
        if (subqueryBaseName == null) {
            throw new IllegalStateException("Missing rendered base name for subquery relation: " + alias);
        }
        return subqueryBaseName;
    }

    private void addIdCTE(RenderContext ctx, String baseName) {
        var ps = new PlainSelect();
        ps.addSelectItem(column("id"));
        ps.setFromItem(table("all_ids_" + baseName));

        ctx.addCTE(idTable(baseName), ps.toString());
    }

    private void addAttributeCTEs(
        RenderContext ctx,
        String baseName,
        Product product,
        Map<String, String> subqueryBaseNames
    ) {
        var relations = product.relations();
        IntStream.range(0, relations.size())
            .forEach(relIndex -> {
                var rel = relations.get(relIndex);

                switch (rel) {
                    case Relation.Table(var tableName, var alias, var attrs) ->
                        attrs.forEach(attr -> addAttributeCTE(
                            ctx,
                            baseName,
                            rel,
                            attr,
                            relIndex + 1,
                            subqueryBaseNames
                        ));
                    case Relation.Subquery(var alias, _, var attrs) ->
                        attrs.forEach(attr -> addAttributeCTE(
                            ctx,
                            baseName,
                            rel,
                            attr,
                            relIndex + 1,
                            subqueryBaseNames
                        ));
                }
            });
    }

    private void addAttributeCTE(
        RenderContext ctx,
        String baseName,
        Relation relation,
        String attr,
        int idIndex,
        Map<String, String> subqueryBaseNames
    ) {
        var qualifiedAttr = relation.alias() + "_" + attr;
        var allIdsTbl = table("all_ids_" + baseName);
        var attrTbl = sourceAttrTableFor(relation, attr, subqueryBaseNames);

        var ps = new PlainSelect();
        ps.addSelectItem(column(allIdsTbl, "id"));
        ps.addSelectItem(column(attrTbl, "v"));
        ps.setFromItem(allIdsTbl);
        ps.addJoins(join(
            attrTbl,
            new EqualsTo(
                column(allIdsTbl, "id" + idIndex),
                column(attrTbl, "id")
            )
        ));

        ctx.addCTE(attrTable(baseName, qualifiedAttr), ps.toString());
    }
}
