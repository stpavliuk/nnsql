package nnsql.tpch.framework;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;

final class TpchRowIdGenerator {

    private TpchRowIdGenerator() {
    }

    static String rowId(
        List<String> headers,
        List<String> values,
        List<String> primaryKeyColumns,
        int rowIndex
    ) {
        if (headers.size() != values.size()) {
            throw new IllegalArgumentException("Header/value count mismatch");
        }

        var row = new LinkedHashMap<String, String>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), values.get(i));
        }

        var canonical = new StringBuilder();
        if (!primaryKeyColumns.isEmpty()) {
            for (var primaryKeyColumn : primaryKeyColumns) {
                appendSegment(canonical, primaryKeyColumn, requiredValue(row, primaryKeyColumn));
            }
        } else {
            for (var header : headers) {
                appendSegment(canonical, header, row.get(header));
            }
            appendSegment(canonical, "__row_index", Integer.toString(rowIndex));
        }

        return md5Uuid(canonical.toString());
    }

    private static String requiredValue(LinkedHashMap<String, String> row, String columnName) {
        var value = row.get(columnName);
        if (value == null || value.isBlank() || "\\N".equals(value.trim())) {
            throw new IllegalArgumentException("Primary key column '%s' is blank".formatted(columnName));
        }
        return value;
    }

    private static void appendSegment(StringBuilder canonical, String columnName, String rawValue) {
        if (!canonical.isEmpty()) {
            canonical.append('|');
        }

        var value = rawValue == null ? "" : rawValue;
        canonical
            .append(columnName)
            .append('=')
            .append(value.length())
            .append(':')
            .append(value);
    }

    private static String md5Uuid(String value) {
        try {
            var digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder(digest.length * 2);
            for (var b : digest) {
                var unsigned = b & 0xFF;
                hex.append(Character.forDigit((unsigned >>> 4) & 0xF, 16));
                hex.append(Character.forDigit(unsigned & 0xF, 16));
            }
            return "%s-%s-%s-%s-%s".formatted(
                hex.substring(0, 8),
                hex.substring(8, 12),
                hex.substring(12, 16),
                hex.substring(16, 20),
                hex.substring(20)
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is unavailable", e);
        }
    }
}
