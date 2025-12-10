package nnsql.query;

import nnsql.query.builder.SQLToIRBuilder;
import nnsql.query.ir.*;
import nnsql.query.renderer.IRRenderer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import parser.sql.sqlLexer;
import parser.sql.sqlParser;

public record QueryTranslator(SQLToIRBuilder irBuilder, IRRenderer renderer) {
    public QueryTranslator(SchemaRegistry irBuilder, IRRenderer renderer) {
        this(new SQLToIRBuilder(irBuilder), renderer);
    }

    public String translate(String sqlQuery) {
        var queryCtx = parseSQL(sqlQuery);
        var selectStmt = queryCtx.selectStmt(0);
        var ir = irBuilder.buildFromSelectStmt(selectStmt);
        return renderer.render(ir);
    }

    public IRNode toIR(String sqlQuery) {
        var queryCtx = parseSQL(sqlQuery);
        var selectStmt = queryCtx.selectStmt(0);
        return irBuilder.buildFromSelectStmt(selectStmt);
    }

    private static sqlParser.QueryContext parseSQL(String sqlQuery) {
        var lexer = new sqlLexer(CharStreams.fromString(sqlQuery));
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new sqlParser(tokenStream);
        return parser.query();
    }
}
