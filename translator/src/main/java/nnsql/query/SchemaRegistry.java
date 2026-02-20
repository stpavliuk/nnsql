package nnsql.query;

import java.util.*;

public class SchemaRegistry {
    private final Map<String, TableSchema> tableSchemas = new HashMap<>();

    public SchemaRegistry() {}

    public void registerTable(String tableName, List<String> attributes) {
        tableSchemas.put(tableName.toUpperCase(),
            new TableSchema(tableName, attributes, new HashMap<>(), new ArrayList<>()));
    }

    public void registerTable(String tableName, List<String> attributes,
                             Map<String, String> columnTypes, List<String> primaryKeyColumns) {
        tableSchemas.put(tableName.toUpperCase(),
            new TableSchema(tableName, attributes, columnTypes, primaryKeyColumns));
    }

    public List<String> getAttributes(String tableName) {
        TableSchema schema = tableSchemas.get(tableName.toUpperCase());
        if (schema == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(schema.attributes());
    }

    public TableSchema getSchema(String tableName) {
        return tableSchemas.get(tableName.toUpperCase());
    }

    public List<String> getPrimaryKeyColumns(String tableName) {
        TableSchema schema = tableSchemas.get(tableName.toUpperCase());
        if (schema == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(schema.primaryKeyColumns());
    }

    public String getIdType(String tableName) {
        TableSchema schema = tableSchemas.get(tableName.toUpperCase());
        if (schema == null) {
            return "VARCHAR(64)";
        }

        List<String> pkColumns = schema.primaryKeyColumns();
        if (pkColumns.isEmpty()) {
            return "VARCHAR(64)";
        } else if (pkColumns.size() == 1) {
            String pkColumn = pkColumns.get(0);
            String dataType = schema.columnTypes().get(pkColumn);
            return dataType != null ? dataType : "VARCHAR(64)";
        } else {
            return "VARCHAR(255)";
        }
    }

    public boolean hasTable(String tableName) {
        return tableSchemas.containsKey(tableName.toUpperCase());
    }

    public void clear() {
        tableSchemas.clear();
    }

    public Set<String> getTableNames() {
        return new HashSet<>(tableSchemas.keySet());
    }

    public List<String> listTables() {
        return new ArrayList<>(tableSchemas.keySet());
    }

    public record TableSchema(
        String tableName,
        List<String> attributes,
        Map<String, String> columnTypes,
        List<String> primaryKeyColumns
    ) {}
}
