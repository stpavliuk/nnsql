package nnsql.data;

import nnsql.query.SchemaRegistry;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DataTranslator {
    private final SchemaRegistry schemaRegistry;

    public DataTranslator(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void translate(String tableName, Path sourceCsv, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        var schema = schemaRegistry.getSchema(tableName);
        var pkColumns = schema != null ? schema.primaryKeyColumns() : List.<String>of();

        try (var reader = new BufferedReader(new FileReader(sourceCsv.toFile()))) {
            var headerLine = reader.readLine();
            if (headerLine == null) throw new IOException("Empty CSV file: " + sourceCsv);

            var headers = parseCsvLine(headerLine);
            var pkIndex = pkColumns.isEmpty() ? -1 : headers.indexOf(pkColumns.getFirst());

            var idWriter = openWriter(outputDir.resolve(tableName + "__ID.csv"));
            idWriter.write("id");
            idWriter.newLine();

            var attrWriters = new LinkedHashMap<String, BufferedWriter>();
            for (var header : headers) {
                var w = openWriter(outputDir.resolve(tableName + "_" + header + ".csv"));
                w.write("id,v");
                w.newLine();
                attrWriters.put(header, w);
            }

            try {
                String line;
                int rowIndex = 0;
                while ((line = reader.readLine()) != null) {
                    var values = parseCsvLine(line);
                    if (values.size() != headers.size()) continue;

                    var rowId = generateRowId(headers, values, pkIndex, rowIndex);

                    idWriter.write(csvEscape(rowId));
                    idWriter.newLine();

                    for (int i = 0; i < headers.size(); i++) {
                        var value = values.get(i);
                        if (isNullOrEmpty(value)) continue;
                        var w = attrWriters.get(headers.get(i));
                        w.write(csvEscape(rowId) + "," + csvEscape(value));
                        w.newLine();
                    }

                    rowIndex++;
                }
            } finally {
                idWriter.close();
                for (var w : attrWriters.values()) w.close();
            }
        }
    }

    private String generateRowId(List<String> headers, List<String> values, int pkIndex, int rowIndex) {
        if (pkIndex >= 0) {
            return values.get(pkIndex);
        }
        var sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(headers.get(i)).append("=").append(values.get(i));
        }
        sb.append("_row").append(rowIndex);
        return md5(sb.toString());
    }

    private boolean isNullOrEmpty(String value) {
        if (value == null) return true;
        var trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.equals("\\N");
    }

    private static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static BufferedWriter openWriter(Path path) throws IOException {
        return new BufferedWriter(new FileWriter(path.toFile()));
    }

    static List<String> parseCsvLine(String line) {
        var result = new ArrayList<String>();
        var sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        result.add(sb.toString());
        return result;
    }

    private static String md5(String input) {
        try {
            var md = MessageDigest.getInstance("MD5");
            var digest = md.digest(input.getBytes());
            var sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
