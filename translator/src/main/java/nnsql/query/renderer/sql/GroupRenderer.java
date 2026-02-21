package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
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
        addPassthroughAttributeCTEs(ctx, baseName, inputBaseName, group.groupingAttributes(), Sql::attrTable);
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
                .map(attr -> (Expression) equalityExists(inputBaseName, attr, "a1", "a2",
                    column(inputIdTbl, "id"), column("R1", "id")))
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

    private Expression equalityExists(String inputBaseName, String attr,
                                       String alias1, String alias2,
                                       Expression outerId, Expression innerId) {
        var t1 = tableAlias(attrTable(inputBaseName, attr), alias1);
        var t2 = tableAlias(attrTable(inputBaseName, attr), alias2);

        var ps = new PlainSelect();
        ps.addSelectItem(new AllColumns());
        ps.setFromItem(t1);
        ps.addJoins(simpleJoin(t2));
        ps.setWhere(andAll(List.of(
            new EqualsTo(column(alias1, "id"), outerId),
            new EqualsTo(column(alias2, "id"), innerId),
            new EqualsTo(column(alias1, "v"), column(alias2, "v"))
        )));

        return exists(ps);
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
        boolean hasCaseWhen = ExpressionSqlRenderer.containsCaseWhen(argument);

        var baseIdTbl = table(idTable(baseName));
        var inputIdTbl = tableAlias(idTable(inputBaseName), "input_id");

        var ps = new PlainSelect();
        ps.addSelectItem(column(baseIdTbl, "id"));
        var argumentExpr = ExpressionSqlRenderer.toSqlExpr(argument, inputBaseName);
        ps.addSelectItem(
            fn(functionName, argumentExpr),
            new Alias("v", true)
        );
        ps.setFromItem(baseIdTbl);

        var joins = new ArrayList<Join>();
        joins.add(simpleJoin(inputIdTbl));

        var conditions = new ArrayList<Expression>();
        addComputedExprAttributeJoins(
            inputBaseName,
            columns,
            column("input_id", "id"),
            hasCaseWhen,
            Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
            joins,
            conditions
        );
        ps.setJoins(joins);

        Expression where = conditions.isEmpty()
            ? new BooleanValue(true)
            : andAll(conditions);

        if (!groupingAttrs.isEmpty()) {
            var equalityConditions = groupingAttrs.stream()
                .map(attr -> (Expression) equalityExists(inputBaseName, attr, "g1", "g2",
                    column("input_id", "id"), column(idTable(baseName), "id")))
                .toList();
            where = and(where, andAll(equalityConditions));
        }

        if (hasCaseWhen && !"COUNT".equals(functionName)) {
            var isNotNull = new IsNullExpression();
            isNotNull.setLeftExpression(argumentExpr);
            isNotNull.setNot(true);
            where = and(where, isNotNull);
        }
        ps.setWhere(where);

        ps.addGroupByColumnReference(column(baseIdTbl, "id"));

        return ps;
    }

    private String renderCountAggregate(String baseName, String inputBaseName, IRExpression argument,
                                         List<String> columns, List<String> groupingAttrs) {
        boolean hasCaseWhen = ExpressionSqlRenderer.containsCaseWhen(argument);

        var countSelect = buildAggregateSelect(baseName, inputBaseName, argument, columns, "COUNT", groupingAttrs);

        var baseIdTbl = table(idTable(baseName));
        var inputIdTbl = tableAlias(idTable(inputBaseName), "input_id");

        var joins = new ArrayList<Join>();
        var conditions = new ArrayList<Expression>();

        addComputedExprAttributeJoins(
            inputBaseName,
            columns,
            column("input_id", "id"),
            hasCaseWhen,
            Sql.NonCaseJoinMode.SIMPLE_JOIN_WITH_WHERE_ID,
            joins,
            conditions
        );

        Expression subWhere = conditions.isEmpty()
            ? new BooleanValue(true)
            : andAll(conditions);

        if (!groupingAttrs.isEmpty()) {
            var equalityConditions = groupingAttrs.stream()
                .map(attr -> (Expression) equalityExists(inputBaseName, attr, "g1", "g2",
                    column("input_id", "id"), column(idTable(baseName), "id")))
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
