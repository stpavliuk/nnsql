package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;

import nnsql.query.ir.DuplElim;
import nnsql.query.renderer.RenderContext;

import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

class DuplElimRenderer {

    void render(DuplElim duplElim, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName, duplElim);
        addPassthroughAttributeCTEs(ctx, baseName, inputBaseName, duplElim.attributes(), Sql::attrCTE);
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName, DuplElim duplElim) {
        var inputIdTbl = table(idTable(inputBaseName));
        var r1Tbl = tableAlias(idTable(inputBaseName), "R1");

        var equalityConditions = duplElim.attributes().stream()
            .map(attr -> (Expression) generateEqualityCondition(inputBaseName, attr))
            .toList();

        var subquery = new PlainSelect();
        subquery.addSelectItem(new AllColumns());
        subquery.setFromItem(r1Tbl);

        Expression subWhere = new net.sf.jsqlparser.expression.operators.relational.MinorThan(
            column("R1", "id"), column(inputIdTbl, "id"));
        for (var cond : equalityConditions) {
            subWhere = and(subWhere, cond);
        }
        subquery.setWhere(subWhere);

        var ps = new PlainSelect();
        ps.addSelectItem(column(inputIdTbl, "id"));
        ps.setFromItem(inputIdTbl);
        ps.setWhere(notExists(subquery));

        ctx.addCTE(idTable(baseName), ps.toString());
    }

    private Expression generateEqualityCondition(String relationName, String attr) {
        var attrTableName = attrCTE(relationName, attr);
        var temp1 = tableAlias(attrTableName, "TEMP1");
        var temp2 = tableAlias(attrTableName, "TEMP2");
        var idTbl = table(idTable(relationName));

        // EXISTS: both rows have matching values
        var existsSelect = new PlainSelect();
        existsSelect.addSelectItem(new AllColumns());
        existsSelect.setFromItem(temp1);
        existsSelect.addJoins(simpleJoin(temp2));
        existsSelect.setWhere(andAll(List.of(
            new EqualsTo(column("TEMP1", "id"), column(idTbl, "id")),
            new EqualsTo(column("TEMP2", "id"), column("R1", "id")),
            new EqualsTo(column("TEMP1", "v"), column("TEMP2", "v"))
        )));

        // NOT EXISTS: neither row has a value for this attribute
        var notExistsSelect = new PlainSelect();
        notExistsSelect.addSelectItem(new AllColumns());
        notExistsSelect.setFromItem(table(attrTableName));
        notExistsSelect.setWhere(or(
            new EqualsTo(column(attrTableName, "id"), column(idTbl, "id")),
            new EqualsTo(column(attrTableName, "id"), column("R1", "id"))
        ));

        return paren(or(exists(existsSelect), notExists(notExistsSelect)));
    }
}
