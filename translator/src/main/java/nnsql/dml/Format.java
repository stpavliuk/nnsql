package nnsql.dml;

final class Format {
    private Format() {}

    static String insertId(String tableName, String idValue) {
        return "INSERT INTO %s__ID (id) VALUES (%s)".formatted(tableName, idValue);
    }

    static String insertAttribute(String tableName, String attributeName, String idValue, String attributeValue) {
        return "INSERT INTO %s_%s (id, v) VALUES (%s, %s)"
            .formatted(tableName, attributeName, idValue, attributeValue);
    }
}
