package nnsql.dml;

import nnsql.Tranlator;
import nnsql.query.SchemaRegistry;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record DMLTranslator(SchemaRegistry schemaRegistry) implements Tranlator {

    public String translate(String dmlScript) {
        return DMLParser
            .parse(dmlScript)
            .stream()
            .map(this::translateInsert)
            .collect(Collectors.joining("\n\n"));
    }

    private String translateInsert(DMLParser.InsertStmt insertStmt) {
        var tableName = insertStmt.tableName();
        var columnNames = insertStmt.columnNames();
        var valueRows = insertStmt.valueRows();

        if (!schemaRegistry.hasTable(tableName)) {
            throw new IllegalArgumentException(
                "Table '%s' not found in schema registry. Run DDL translation first."
                    .formatted(tableName)
            );
        }

        var schema = schemaRegistry.getSchema(tableName);
        var primaryKeyColumns = schema.primaryKeyColumns();

        for (int rowIndex = 0; rowIndex < valueRows.size(); rowIndex++) {
            var values = valueRows.get(rowIndex);
            if (values.size() != columnNames.size()) {
                throw new IllegalArgumentException(
                    "Column count mismatch in row %d: %d columns specified, but %d values provided"
                        .formatted(rowIndex, columnNames.size(), values.size())
                );
            }
        }

        var statements = new ArrayList<String>();

        for (int rowIndex = 0; rowIndex < valueRows.size(); rowIndex++) {
            var values = valueRows.get(rowIndex);
            var row = buildRow(columnNames, values);
            var rowId = generateRowId(tableName, row, primaryKeyColumns, rowIndex);

            statements.add(Format.insertId(tableName, rowId));

            for (var entry : row.entrySet()) {
                var columnName = entry.getKey();
                var value = entry.getValue();

                if (!value.isNull()) {
                    statements.add(Format.insertAttribute(tableName, columnName, rowId, value.toSQL()));
                }
            }
        }

        return String.join(";\n", statements) + ";";
    }

    private String generateRowId(String tableName, Map<String, DMLParser.LiteralValue> row,
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

    private Map<String, DMLParser.LiteralValue> buildRow(
        List<String> columnNames,
        List<DMLParser.LiteralValue> values
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
}
