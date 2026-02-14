package nnsql.query;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import nnsql.query.builder.Pipelines;
import nnsql.query.ir.IRNode;
import nnsql.query.renderer.IRRenderer;

import java.util.concurrent.atomic.AtomicInteger;

public record QueryTranslator(SchemaRegistry schema, IRRenderer renderer) {

    public String translate(String sqlQuery) {
        return renderer.render(toIR(sqlQuery));
    }

    public IRNode toIR(String sqlQuery) {
        var select = parseSelect(sqlQuery);
        return Pipelines.buildQuery(select, schema, new AtomicInteger(0));
    }

    private static PlainSelect parseSelect(String sql) {
        try {
            return (PlainSelect) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SQL: " + sql, e);
        }
    }
}
