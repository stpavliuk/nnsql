package nnsql.query;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import nnsql.query.builder.IRBuilder;
import nnsql.query.ir.IRNode;
import nnsql.query.optim.JoinPredicatePushdown;
import nnsql.query.renderer.IRRenderer;

public record QueryTranslator(IRBuilder irBuilder, IRRenderer renderer) {
    public QueryTranslator(SchemaRegistry schema, IRRenderer renderer) {
        this(new IRBuilder(schema), renderer);
    }

    public String translate(String sqlQuery) {
        var select = parseSelect(sqlQuery);
        var ir = JoinPredicatePushdown.optimize(irBuilder.build(select));
        return renderer.render(ir);
    }

    public IRNode toIR(String sqlQuery) {
        var select = parseSelect(sqlQuery);
        return irBuilder.build(select);
    }

    private static PlainSelect parseSelect(String sql) {
        try {
            return (PlainSelect) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SQL: " + sql, e);
        }
    }
}
