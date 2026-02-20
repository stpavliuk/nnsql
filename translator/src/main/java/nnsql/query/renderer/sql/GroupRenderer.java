package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.IRExpression;
import nnsql.query.ir.Group;
import nnsql.query.renderer.RenderContext;

import java.util.ArrayList;
import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

class GroupRenderer {

    void render(Group group, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName, group);
        addGroupingAttributeCTEs(ctx, baseName, inputBaseName, group.groupingAttributes());
        addAggregateCTEs(ctx, baseName, inputBaseName, group);
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName, Group group) {
        var groupingAttrs = group.groupingAttributes();

        if (groupingAttrs.isEmpty()) {
            var ps = new PlainSelect();
            ps.addSelectItem(fn("MIN", column("id")), new Alias("id", true));
            ps.setFromItem(table(idTable(inputBaseName)));

            ctx.addCTE(idTable(baseName), ps.toString());
        } else {
            var inputIdTbl = table(idTable(inputBaseName));
            var r1Tbl = tableAlias(idTable(inputBaseName), "R1");

            var equalityConditions = groupingAttrs.stream()
                .map(attr -> (Expression) groupEqualityExists(inputBaseName, attr, inputIdTbl, "R1"))
                .toList();

            var subquery = new PlainSelect();
            subquery.addSelectItem(new AllColumns());
            subquery.setFromItem(r1Tbl);
            subquery.setWhere(and(
                new net.sf.jsqlparser.expression.operators.relational.MinorThan(
                    column("R1", "id"), column(inputIdTbl, "id")),
                andAll(equalityConditions)));

            var ps = new PlainSelect();
            ps.addSelectItem(column(inputIdTbl, "id"));
            ps.setFromItem(inputIdTbl);
            ps.setWhere(notExists(subquery));

            ctx.addCTE(idTable(baseName), ps.toString());
        }
    }

    private Expression groupEqualityExists(String inputBaseName, String attr,
                                            Table outerIdTable, String innerAlias) {
        var a1 = tableAlias(attrTable(inputBaseName, attr), "a1");
        var a2 = tableAlias(attrTable(inputBaseName, attr), "a2");

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(a1);
        ps.addJoins(simpleJoin(a2));
        ps.setWhere(andAll(List.of(
            new EqualsTo(column("a1", "id"), column(outerIdTable, "id")),
            new EqualsTo(column("a2", "id"), column(innerAlias, "id")),
            new EqualsTo(column("a1", "v"), column("a2", "v"))
        )));

        return exists(ps);
    }

    private void addGroupingAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                           List<String> groupingAttributes) {
        for (var attr : groupingAttributes) {
            var inputAttrTbl = table(attrTable(inputBaseName, attr));
            var baseIdTbl = table(idTable(baseName));

            var ps = new PlainSelect();
            ps.addSelectItem(new AllTableColumns(inputAttrTbl));
            ps.setFromItem(inputAttrTbl);
            ps.addJoins(join(baseIdTbl,
                new EqualsTo(column(baseIdTbl, "id"), column(inputAttrTbl, "id"))));

            ctx.addCTE(attrTable(baseName, attr), ps.toString());
        }
    }

    private void addAggregateCTEs(RenderContext ctx, String baseName, String inputBaseName, Group group) {
        var groupingAttrs = group.groupingAttributes();

        for (var aggregate : group.aggregates()) {
            var functionName = aggregate.function();
            var argument = aggregate.argument();
            var alias = aggregate.alias();

            var columns = ExpressionSqlRenderer.collectColumns(argument);

            String definition;
            if (functionName.equals("COUNT")) {
                definition = renderCountAggregate(baseName, inputBaseName, argument, columns, groupingAttrs);
            } else {
                definition = buildAggregateSelect(baseName, inputBaseName, argument, columns, functionName, groupingAttrs)
                    .toString();
            }

            ctx.addCTE(attrTable(baseName, alias), definition);
        }
    }

    private PlainSelect buildAggregateSelect(String baseName, String inputBaseName, IRExpression argument,
                                              List<String> columns, String functionName,
                                              List<String> groupingAttrs) {
        var baseIdTbl = table(idTable(baseName));
        var inputIdTbl = tableAlias(idTable(inputBaseName), "input_id");

        var ps = new PlainSelect();
        ps.addSelectItem(column(baseIdTbl, "id"));
        ps.addSelectItem(
            fn(functionName, ExpressionSqlRenderer.toSqlExpr(argument, inputBaseName)),
            new Alias("v", true)
        );
        ps.setFromItem(baseIdTbl);

        var joins = new ArrayList<Join>();
        joins.add(simpleJoin(inputIdTbl));

        var conditions = new ArrayList<Expression>();
        for (var col : columns) {
            var attrTbl = table(attrTable(inputBaseName, col));
            joins.add(simpleJoin(attrTbl));
            conditions.add(new EqualsTo(column(attrTbl, "id"), column("input_id", "id")));
        }
        ps.setJoins(joins);

        Expression where = conditions.isEmpty()
            ? new BooleanValue(true)
            : andAll(conditions);

        if (!groupingAttrs.isEmpty()) {
            var equalityConditions = groupingAttrs.stream()
                .map(attr -> (Expression) aggEqualityExists(inputBaseName, attr, baseName))
                .toList();
            where = and(where, andAll(equalityConditions));
        }
        ps.setWhere(where);

        ps.addGroupByColumnReference(column(baseIdTbl, "id"));

        return ps;
    }

    private Expression aggEqualityExists(String inputBaseName, String attr, String baseName) {
        var g1 = tableAlias(attrTable(inputBaseName, attr), "g1");
        var g2 = tableAlias(attrTable(inputBaseName, attr), "g2");

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(g1);
        ps.addJoins(simpleJoin(g2));
        ps.setWhere(andAll(List.of(
            new EqualsTo(column("g1", "id"), column("input_id", "id")),
            new EqualsTo(column("g2", "id"), column(idTable(baseName), "id")),
            new EqualsTo(column("g1", "v"), column("g2", "v"))
        )));

        return exists(ps);
    }

    private String renderCountAggregate(String baseName, String inputBaseName, IRExpression argument,
                                         List<String> columns, List<String> groupingAttrs) {
        var countSelect = buildAggregateSelect(baseName, inputBaseName, argument, columns, "COUNT", groupingAttrs);

        var baseIdTbl = table(idTable(baseName));
        var inputIdTbl = tableAlias(idTable(inputBaseName), "input_id");

        var joins = new ArrayList<Join>();
        var conditions = new ArrayList<Expression>();

        for (var col : columns) {
            var attrTbl = table(attrTable(inputBaseName, col));
            joins.add(simpleJoin(attrTbl));
            conditions.add(new EqualsTo(column(attrTbl, "id"), column("input_id", "id")));
        }

        Expression subWhere = conditions.isEmpty()
            ? new BooleanValue(true)
            : andAll(conditions);

        if (!groupingAttrs.isEmpty()) {
            var equalityConditions = groupingAttrs.stream()
                .map(attr -> (Expression) aggEqualityExists(inputBaseName, attr, baseName))
                .toList();
            subWhere = and(subWhere, andAll(equalityConditions));
        } else if (!conditions.isEmpty()) {
            subWhere = and(subWhere, new BooleanValue(true));
        }

        var subquery = new PlainSelect();
        subquery.addSelectItem(new AllColumns());
        subquery.setFromItem(inputIdTbl);
        if (!joins.isEmpty()) {
            subquery.setJoins(joins);
        }
        subquery.setWhere(subWhere);

        var zeroPart = new PlainSelect();
        zeroPart.addSelectItem(column(baseIdTbl, "id"));
        zeroPart.addSelectItem(new LongValue(0), new Alias("v", true));
        zeroPart.setFromItem(baseIdTbl);
        zeroPart.setWhere(notExists(subquery));

        var union = new SetOperationList();
        union.addSelects(countSelect, zeroPart);
        union.addOperations(new UnionOp());

        return union.toString();
    }
}
