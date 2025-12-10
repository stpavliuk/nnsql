package nnsql.ddl;

import nnsql.util.Option;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import parser.ddl.ddlLexer;
import parser.ddl.ddlParser;

import java.util.ArrayList;
import java.util.List;

final class DDLParser {
    private DDLParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    static List<TableDef> parse(String ddlScript) {
        var parseTree = parseDDL(ddlScript);
        var tables = new ArrayList<TableDef>();

        for (var createTableCtx : parseTree.createTableStmt()) {
            tables.add(parseCreateTable(createTableCtx));
        }

        return tables;
    }

    private static TableDef parseCreateTable(ddlParser.CreateTableStmtContext ctx) {
        var tableName = extractIdentifier(ctx.tableName().identifier());
        var tableInfo = parseTableElements(ctx.tableElementList());

        return new TableDef(tableName, tableInfo.columns(), tableInfo.primaryKeyColumn());
    }

    private static TableInfo parseTableElements(ddlParser.TableElementListContext ctx) {
        var columns = new ArrayList<ColumnDef>();
        Option<String> primaryKeyColumn = Option.none();

        for (var columnDef : ctx.columnDef()) {
            var columnName = extractIdentifier(columnDef.columnName().identifier());
            var dataType = extractDataType(columnDef.dataType());

            columns.add(new ColumnDef(columnName, dataType));

            for (var constraint : columnDef.columnConstraint()) {
                if (constraint.PRIMARY() != null) {
                    if (primaryKeyColumn.isSome()) {
                        throw new IllegalArgumentException(
                            "Multiple PRIMARY KEY constraints not supported. Found on both '%s' and '%s'"
                                .formatted(primaryKeyColumn.get(), columnName)
                        );
                    }
                    primaryKeyColumn = Option.some(columnName);
                }
            }
        }

        return new TableInfo(columns, primaryKeyColumn);
    }

    private static String extractDataType(ddlParser.DataTypeContext ctx) {
        return switch (ctx) {
            case ddlParser.IntTypeContext _ -> "INT";
            case ddlParser.IntegerTypeContext _ -> "INTEGER";
            case ddlParser.SmallintTypeContext _ -> "SMALLINT";
            case ddlParser.BigintTypeContext _ -> "BIGINT";
            case ddlParser.TinyintTypeContext _ -> "TINYINT";
            case ddlParser.FloatTypeContext _ -> "FLOAT";
            case ddlParser.DoubleTypeContext _ -> "DOUBLE";
            case ddlParser.DecimalTypeContext dt -> formatDecimalType(dt, "DECIMAL");
            case ddlParser.NumericTypeContext dt -> formatNumericType(dt, "NUMERIC");
            case ddlParser.CharTypeContext ct -> formatStringType(ct.length(), "CHAR");
            case ddlParser.VarcharTypeContext vt -> formatStringType(vt.length(), "VARCHAR");
            case ddlParser.TextTypeContext _ -> "TEXT";
            case ddlParser.DateTypeContext _ -> "DATE";
            case ddlParser.TimeTypeContext _ -> "TIME";
            case ddlParser.DatetimeTypeContext _ -> "DATETIME";
            case ddlParser.TimestampTypeContext _ -> "TIMESTAMP";
            case ddlParser.BooleanTypeContext _, ddlParser.BoolTypeContext _ -> "BOOLEAN";
            case ddlParser.BlobTypeContext _ -> "BLOB";
            default -> throw new IllegalArgumentException("Unsupported data type: " + ctx.getText());
        };
    }

    private static String formatDecimalType(ddlParser.DecimalTypeContext ctx, String typeName) {
        if (ctx.precision() != null) {
            var precision = ctx.precision().getText();
            if (ctx.scale() != null) {
                var scale = ctx.scale().getText();
                return "%s(%s, %s)".formatted(typeName, precision, scale);
            }
            return "%s(%s)".formatted(typeName, precision);
        }
        return typeName;
    }

    private static String formatNumericType(ddlParser.NumericTypeContext ctx, String typeName) {
        if (ctx.precision() != null) {
            var precision = ctx.precision().getText();
            if (ctx.scale() != null) {
                var scale = ctx.scale().getText();
                return "%s(%s, %s)".formatted(typeName, precision, scale);
            }
            return "%s(%s)".formatted(typeName, precision);
        }
        return typeName;
    }

    private static String formatStringType(ddlParser.LengthContext lengthCtx, String typeName) {
        if (lengthCtx != null) {
            return "%s(%s)".formatted(typeName, lengthCtx.getText());
        }
        return typeName;
    }

    private static String extractIdentifier(ddlParser.IdentifierContext ctx) {
        if (ctx.QUOTED_IDENT() != null) {
            var text = ctx.QUOTED_IDENT().getText();
            return text.substring(1, text.length() - 1);
        }
        return ctx.IDENT().getText();
    }

    private static ddlParser.DdlStatementContext parseDDL(String ddlScript) {
        var lexer = new ddlLexer(CharStreams.fromString(ddlScript));
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new ddlParser(tokenStream);
        return parser.ddlStatement();
    }

    record TableDef(
        String name,
        List<ColumnDef> columns,
        Option<String> primaryKeyColumn
    ) {}

    private record TableInfo(
        List<ColumnDef> columns,
        Option<String> primaryKeyColumn
    ) {}

    record ColumnDef(
        String name,
        String dataType
    ) {}
}
