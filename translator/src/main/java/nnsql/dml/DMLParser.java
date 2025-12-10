package nnsql.dml;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import parser.dml.dmlLexer;
import parser.dml.dmlParser;

import java.util.List;

final class DMLParser {

    static List<InsertStmt> parse(String dmlScript) {
        var parseTree = parseDML(dmlScript);

        return parseTree
            .insertStmt()
            .stream()
            .map(DMLParser::parseInsert)
            .toList();
    }

    private static InsertStmt parseInsert(dmlParser.InsertStmtContext ctx) {
        var tableName = extractIdentifier(ctx.tableName().identifier());
        var columnNames = extractColumnNames(ctx.columnList());
        var valueRows = extractValueRows(ctx.valuesList());

        return new InsertStmt(tableName, columnNames, valueRows);
    }

    private static List<String> extractColumnNames(dmlParser.ColumnListContext ctx) {
        return ctx
            .columnName()
            .stream()
            .map(colCtx -> extractIdentifier(colCtx.identifier()))
            .toList();
    }

    private static List<List<LiteralValue>> extractValueRows(dmlParser.ValuesListContext ctx) {
        return ctx
            .literalList()
            .stream()
            .map(DMLParser::extractLiterals)
            .toList();
    }

    private static List<LiteralValue> extractLiterals(dmlParser.LiteralListContext ctx) {
        return ctx
            .literal().stream()
            .map(DMLParser::extractLiteral)
            .toList();
    }

    private static LiteralValue extractLiteral(dmlParser.LiteralContext ctx) {
        return switch (ctx.start.getType()) {
            case dmlParser.NULL_T -> LiteralValue.nullValue();
            case dmlParser.NUMBER -> LiteralValue.number(ctx.start.getText());
            case dmlParser.STRING -> {
                var text = ctx.start.getText();
                yield LiteralValue.string(text.substring(1, text.length() - 1));
            }
            case dmlParser.TRUE -> LiteralValue.bool(true);
            case dmlParser.FALSE -> LiteralValue.bool(false);
            default -> throw new IllegalArgumentException("Unknown literal type: " + ctx.getText());
        };
    }

    private static String extractIdentifier(dmlParser.IdentifierContext ctx) {
        if (ctx.QUOTED_IDENT() != null) {
            var text = ctx.QUOTED_IDENT().getText();
            return text.substring(1, text.length() - 1);
        }
        return ctx.IDENT().getText();
    }

    private static dmlParser.DmlStatementContext parseDML(String dmlScript) {
        var lexer = new dmlLexer(CharStreams.fromString(dmlScript));
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new dmlParser(tokenStream);
        return parser.dmlStatement();
    }

    sealed interface LiteralValue {
        String toSQL();

        String toHashInput();

        boolean isNull();

        static LiteralValue number(String value) {
            return new NumberLiteral(value);
        }

        static LiteralValue string(String value) {
            return new StringLiteral(value);
        }

        static LiteralValue bool(boolean value) {
            return new BoolLiteral(value);
        }

        static LiteralValue nullValue() {
            return new NullLiteral();
        }

        record NumberLiteral(String value) implements LiteralValue {
            @Override
            public String toSQL() {
                return value;
            }

            @Override
            public String toHashInput() {
                return value;
            }

            @Override
            public boolean isNull() {
                return false;
            }
        }

        record StringLiteral(String value) implements LiteralValue {
            @Override
            public String toSQL() {
                return "'" + value.replace("'", "''") + "'";
            }

            @Override
            public String toHashInput() {
                return value;
            }

            @Override
            public boolean isNull() {
                return false;
            }
        }

        record BoolLiteral(boolean value) implements LiteralValue {
            @Override
            public String toSQL() {
                return value ? "TRUE" : "FALSE";
            }

            @Override
            public String toHashInput() {
                return String.valueOf(value);
            }

            @Override
            public boolean isNull() {
                return false;
            }
        }

        record NullLiteral() implements LiteralValue {
            @Override
            public String toSQL() {
                return "NULL";
            }

            @Override
            public String toHashInput() {
                return "NULL";
            }

            @Override
            public boolean isNull() {
                return true;
            }
        }
    }

    record InsertStmt(
        String tableName,
        List<String> columnNames,
        List<List<LiteralValue>> valueRows
    ) {
    }
}
