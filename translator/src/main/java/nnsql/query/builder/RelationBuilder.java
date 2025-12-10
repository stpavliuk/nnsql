package nnsql.query.builder;

import nnsql.query.SchemaRegistry;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Relation;
import parser.sql.sqlParser;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public record RelationBuilder(
    SchemaRegistry schemaRegistry,
    Function<sqlParser.SelectStmtContext, IRNode> subqueryBuilder,
    AtomicInteger nodeIdCounter
) {

    public Relation build(sqlParser.FromItemContext ctx) {
        return switch (ctx) {
            case sqlParser.FromTableItemContext tableCtx -> buildTable(tableCtx);
            case sqlParser.FromQueryItemContext queryCtx -> buildSubquery(queryCtx);
            default -> throw new IllegalArgumentException("Unknown FROM item type");
        };
    }

    private Relation.Table buildTable(sqlParser.FromTableItemContext ctx) {
        var tableName = ctx.tableName().getText();
        var alias = ctx.alias() != null ? ctx.alias().getText() : tableName;
        var attributes = schemaRegistry.getAttributes(tableName);

        if (attributes.isEmpty()) {
            throw new IllegalArgumentException(
                "Table '%s' not found in schema. Please register the table schema first."
                    .formatted(tableName));
        }

        return new Relation.Table(tableName, alias, attributes);
    }

    private Relation.Subquery buildSubquery(sqlParser.FromQueryItemContext ctx) {
        var alias = ctx.alias() != null
            ? ctx.alias().getText()
            : "subq" + nodeIdCounter.getAndIncrement();
        var subqueryIR = subqueryBuilder.apply(ctx.query().selectStmt(0));
        var attributes = AttributeResolver.collectFrom(subqueryIR);

        return new Relation.Subquery(alias, subqueryIR, attributes);
    }
}
