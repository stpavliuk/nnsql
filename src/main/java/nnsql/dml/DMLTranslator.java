package nnsql.dml;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import nnsql.Translator;
import nnsql.query.SchemaRegistry;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record DMLTranslator(SchemaRegistry schemaRegistry) implements Translator {

    public String translate(String dmlScript) {
        try {
            var statements = CCJSqlParserUtil.parseStatements(dmlScript);
            return statements.stream()
                .filter(Insert.class::isInstance)
                .map(Insert.class::cast)
                .map(this::translateInsert)
                .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DML: " + dmlScript, e);
        }
    }

    private String translateInsert(Insert insert) {
        var tableName = insert.getTable().getName();
        var columnNames = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        if (!schemaRegistry.hasTable(tableName)) {
            throw new IllegalArgumentException(
                "Table '%s' not found in schema registry. Run DDL translation first."
                    .formatted(tableName)
            );
        }

        var schema = schemaRegistry.getSchema(tableName);
        var primaryKeyColumns = schema.primaryKeyColumns();

        var valueRows = extractValueRows(insert);

        for (int rowIndex = 0; rowIndex < valueRows.size(); rowIndex++) {
            var values = valueRows.get(rowIndex);
            if (values.size() != columnNames.size()) {
                throw new IllegalArgumentException(
                    "Column count mismatch in row %d: %d columns specified, but %d values provided"
                        .formatted(rowIndex, columnNames.size(), values.size())
                );
            }
        }

        var stmts = new ArrayList<String>();

        for (int rowIndex = 0; rowIndex < valueRows.size(); rowIndex++) {
            var values = valueRows.get(rowIndex);
            var row = buildRow(columnNames, values);
            var rowId = generateRowId(tableName, row, primaryKeyColumns, rowIndex);

            stmts.add(Format.insertId(tableName, rowId));

            for (var entry : row.entrySet()) {
                var columnName = entry.getKey();
                var value = entry.getValue();

                if (!value.isNull()) {
                    stmts.add(Format.insertAttribute(tableName, columnName, rowId, value.toSQL()));
                }
            }
        }

        return String.join(";\n", stmts) + ";";
    }

    @SuppressWarnings("unchecked")
    private List<List<LiteralValue>> extractValueRows(Insert insert) {
        var values = insert.getValues();
        var expressions = values.getExpressions();

        if (!expressions.isEmpty() && expressions.getFirst() instanceof ParenthesedExpressionList<?>) {
            return expressions.stream()
                .map(e -> ((ParenthesedExpressionList<net.sf.jsqlparser.expression.Expression>) e).stream()
                    .map(v -> toLiteralValue(v))
                    .toList())
                .toList();
        } else {
            return List.of(expressions.stream()
                .map(this::toLiteralValue)
                .toList());
        }
    }

    private LiteralValue toLiteralValue(net.sf.jsqlparser.expression.Expression expr) {
        return switch (expr) {
            case LongValue lv -> LiteralValue.number(String.valueOf(lv.getValue()));
            case DoubleValue dv -> LiteralValue.number(String.valueOf(dv.getValue()));
            case StringValue sv -> LiteralValue.string(sv.getValue());
            case NullValue _ -> LiteralValue.nullValue();
            default -> throw new UnsupportedOperationException(
                "Unsupported literal in INSERT: " + expr.getClass().getSimpleName());
        };
    }

    private String generateRowId(String tableName, Map<String, LiteralValue> row,
                                 List<String> primaryKeyColumns, int rowIndex) {
        if (!primaryKeyColumns.isEmpty()) {
            var pkColumn = primaryKeyColumns.getFirst();
            var pkValue = row.get(pkColumn);

            if (pkValue == null || pkValue.isNull()) {
                throw new IllegalArgumentException(
                    "Primary key column '%s' cannot be NULL".formatted(pkColumn)
                );
            }

            return pkValue.toSQL();
        } else {
            var hashInput = row
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue().toHashInput())
                .collect(Collectors.joining(","));

            var hashWithIndex = hashInput + "_row" + rowIndex;
            return "'" + hashString(hashWithIndex) + "'";
        }
    }

    private String hashString(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("MD5");
            var digest = md.digest(input.getBytes());
            var sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }

    private Map<String, LiteralValue> buildRow(
        List<String> columnNames,
        List<LiteralValue> values
    ) {
        return IntStream
            .range(0, columnNames.size())
            .boxed()
            .collect(Collectors.toMap(
                columnNames::get,
                values::get,
                (a, b) -> b,
                LinkedHashMap::new
            ));
    }

    sealed interface LiteralValue {
        String toSQL();
        String toHashInput();
        boolean isNull();

        static LiteralValue number(String value) { return new NumberLiteral(value); }
        static LiteralValue string(String value) { return new StringLiteral(value); }
        static LiteralValue bool(boolean value) { return new BoolLiteral(value); }
        static LiteralValue nullValue() { return new NullLiteral(); }

        record NumberLiteral(String value) implements LiteralValue {
            @Override public String toSQL() { return value; }
            @Override public String toHashInput() { return value; }
            @Override public boolean isNull() { return false; }
        }

        record StringLiteral(String value) implements LiteralValue {
            @Override public String toSQL() { return "'" + value.replace("'", "''") + "'"; }
            @Override public String toHashInput() { return value; }
            @Override public boolean isNull() { return false; }
        }

        record BoolLiteral(boolean value) implements LiteralValue {
            @Override public String toSQL() { return value ? "TRUE" : "FALSE"; }
            @Override public String toHashInput() { return String.valueOf(value); }
            @Override public boolean isNull() { return false; }
        }

        record NullLiteral() implements LiteralValue {
            @Override public String toSQL() { return "NULL"; }
            @Override public String toHashInput() { return "NULL"; }
            @Override public boolean isNull() { return true; }
        }
    }
}
