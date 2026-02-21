package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.Return;
import nnsql.query.renderer.RenderContext;

import java.util.ArrayList;
import java.util.List;

import static nnsql.query.renderer.sql.Sql.*;

class ReturnRenderer {

    void render(Return returnNode, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName);

        if (!returnNode.selectStar()) {
            addAttributeCTEs(ctx, baseName, inputBaseName, returnNode.selectedAttributes());
        }
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName) {
        var ps = new PlainSelect();
        ps.addSelectItem(column("id"));
        ps.setFromItem(table(idTable(inputBaseName)));

        ctx.addCTE(idTable(baseName), ps.toString());
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                   List<Return.AttributeRef> selectedAttributes) {
        selectedAttributes.forEach(attr -> {
            if (attr.isSimpleColumn()) {
                addSimpleColumnCTE(ctx, baseName, inputBaseName, attr);
            } else {
                addComputedExpressionCTE(ctx, baseName, inputBaseName, attr);
            }
        });
    }

    private void addSimpleColumnCTE(RenderContext ctx, String baseName, String inputBaseName,
                                     Return.AttributeRef attr) {
        var ps = new PlainSelect();
        ps.addSelectItem(column("id"));
        ps.addSelectItem(column("v"));
        ps.setFromItem(table(attrTable(inputBaseName, attr.sourceColumnName())));

        ctx.addCTE(attrCTE(baseName, attr.alias()), ps.toString());
    }

    private void addComputedExpressionCTE(RenderContext ctx, String baseName, String inputBaseName,
                                           Return.AttributeRef attr) {
        var columns = ExpressionSqlRenderer.collectColumns(attr.source());
        if (columns.isEmpty()) {
            throw new IllegalStateException("Computed expression must reference at least one column");
        }

        boolean hasCaseWhen = ExpressionSqlRenderer.containsCaseWhen(attr.source());

        var idTbl = table(idTable(inputBaseName));
        var ps = new PlainSelect();
        ps.addSelectItem(column(idTbl, "id"));

        var exprSql = ExpressionSqlRenderer.toSqlExpr(attr.source(), inputBaseName);
        ps.addSelectItem(exprSql, new Alias("v", true));

        ps.setFromItem(idTbl);

        var joins = new ArrayList<Join>();
        addComputedExprAttributeJoins(
            inputBaseName,
            columns,
            column(idTbl, "id"),
            hasCaseWhen,
            Sql.NonCaseJoinMode.INNER_ON,
            joins,
            new ArrayList<>()
        );
        ps.setJoins(joins);

        if (hasCaseWhen) {
            var whereExpr = ExpressionSqlRenderer.toSqlExpr(attr.source(), inputBaseName);
            var isNotNull = new IsNullExpression();
            isNotNull.setLeftExpression(whereExpr);
            isNotNull.setNot(true);
            ps.setWhere(isNotNull);
        }

        ctx.addCTE(attrCTE(baseName, attr.alias()), ps.toString());
    }
}
