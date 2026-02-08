package nnsql.ddl;

final class Format {
    public static final String VARCHAR_64 = "VARCHAR(64)";

    private Format() {}

    static String idTable(String tableName, String idType) {
        return """
            CREATE TABLE %s__ID (
                id %s PRIMARY KEY
            )""".formatted(tableName, idType);
    }

    static String attributeTable(String tableName, String attributeName, String idType, String valueType) {
        return """
            CREATE TABLE %s_%s (
                id %s,
                v %s,
                PRIMARY KEY (id),
                FOREIGN KEY (id) REFERENCES %s__ID(id)
            )""".formatted(tableName, attributeName, idType, valueType, tableName);
    }
}
