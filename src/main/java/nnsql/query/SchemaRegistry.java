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

    public boolean hasTable(String tableName) {
        return tableSchemas.containsKey(tableName.toUpperCase());
    }

    public record TableSchema(
        String tableName,
        List<String> attributes,
        Map<String, String> columnTypes,
        List<String> primaryKeyColumns
    ) {}
}
