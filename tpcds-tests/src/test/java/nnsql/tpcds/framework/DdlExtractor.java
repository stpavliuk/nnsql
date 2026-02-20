package nnsql.tpcds.framework;

import java.sql.*;
import java.util.*;

public class DdlExtractor {

    public static List<String> extractCreateStatements(Connection conn) throws SQLException {
        var tables = new LinkedHashMap<String, List<ColumnInfo>>();

        try (var rs = conn.createStatement().executeQuery("""
                SELECT table_name, column_name, data_type, is_nullable, ordinal_position
                FROM information_schema.columns
                WHERE table_schema = 'main'
                ORDER BY table_name, ordinal_position
                """)) {
            while (rs.next()) {
                var tableName = rs.getString("table_name");
                tables.computeIfAbsent(tableName, _ -> new ArrayList<>()).add(new ColumnInfo(
                    rs.getString("column_name"),
                    rs.getString("data_type"),
                    rs.getString("is_nullable")
                ));
            }
        }

        var primaryKeys = extractPrimaryKeys(conn);

        var result = new ArrayList<String>();
        for (var entry : tables.entrySet()) {
            var tableName = entry.getKey();
            var columns = entry.getValue();
            var pkColumns = primaryKeys.getOrDefault(tableName, Set.of());
            result.add(buildCreateTable(tableName, columns, pkColumns));
        }
        return result;
    }

    private static Map<String, Set<String>> extractPrimaryKeys(Connection conn) throws SQLException {
        var pks = new HashMap<String, Set<String>>();
        try (var rs = conn.createStatement().executeQuery("""
                SELECT table_name, constraint_column_names, constraint_type
                FROM duckdb_constraints()
                WHERE constraint_type = 'PRIMARY KEY'
                """)) {
            while (rs.next()) {
                var tableName = rs.getString("table_name");
                var colNamesRaw = rs.getString("constraint_column_names");
                var colNames = parseConstraintColumnNames(colNamesRaw);
                pks.computeIfAbsent(tableName, _ -> new LinkedHashSet<>()).addAll(colNames);
            }
        }
        return pks;
    }

    static List<String> parseConstraintColumnNames(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        var trimmed = raw.strip();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return Arrays.stream(trimmed.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static String buildCreateTable(String tableName, List<ColumnInfo> columns, Set<String> pkColumns) {
        var sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");
        for (int i = 0; i < columns.size(); i++) {
            var col = columns.get(i);
            sb.append("    ").append(col.name).append(" ").append(col.dataType);
            if (pkColumns.size() == 1 && pkColumns.contains(col.name)) {
                sb.append(" PRIMARY KEY");
            }
            if (i < columns.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    private record ColumnInfo(String name, String dataType, String isNullable) {}
}
