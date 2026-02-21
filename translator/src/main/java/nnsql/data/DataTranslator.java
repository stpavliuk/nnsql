package nnsql.data;

import nnsql.query.SchemaRegistry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class DataTranslator {
    private static final CSVFormat INPUT_CSV = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();
    private static final CSVFormat OUTPUT_CSV = CSVFormat.DEFAULT.builder()
            .setRecordSeparator(System.lineSeparator())
            .get();

    private final SchemaRegistry schemaRegistry;

    public DataTranslator(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void translate(String tableName, Path sourceCsv, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        try (var reader = Files.newBufferedReader(sourceCsv);
             var parser = INPUT_CSV.parse(reader)) {
            var layout = resolveTableLayout(tableName, parser, sourceCsv);
            try (var writers = OutputWriters.open(outputDir, tableName, layout.headers())) {
                translateRows(parser, layout, writers);
            }
        }
    }

    private TableLayout resolveTableLayout(String tableName, CSVParser parser, Path sourceCsv) throws IOException {
        var headers = readHeaders(parser, sourceCsv);
        return new TableLayout(headers, resolvePrimaryKeyIndex(tableName, headers));
    }

    private static List<String> readHeaders(CSVParser parser, Path sourceCsv) throws IOException {
        var headers = List.copyOf(parser.getHeaderNames());
        if (headers.isEmpty()) {
            throw new IOException("Empty CSV file: " + sourceCsv);
        }
        return headers;
    }

    private int resolvePrimaryKeyIndex(String tableName, List<String> headers) {
        var schema = schemaRegistry.getSchema(tableName);
        if (schema == null || schema.primaryKeyColumns().isEmpty()) {
            return -1;
        }
        return headers.indexOf(schema.primaryKeyColumns().getFirst());
    }

    private void translateRows(CSVParser parser, TableLayout layout, OutputWriters writers) throws IOException {
        int rowIndex = 0;
        for (var record : parser) {
            if (record.size() != layout.headers().size()) {
                continue;
            }
            writeRow(layout, record, rowIndex, writers);
            rowIndex++;
        }
    }

    private void writeRow(TableLayout layout, CSVRecord values, int rowIndex, OutputWriters writers) throws IOException {
        var rowId = generateRowId(layout.headers(), values, layout.primaryKeyIndex(), rowIndex);
        writers.writeId(rowId);
        writeAttributes(layout.headers(), values, rowId, writers);
    }

    private static void writeAttributes(List<String> headers, CSVRecord values, String rowId, OutputWriters writers)
            throws IOException {
        for (int i = 0; i < headers.size(); i++) {
            var value = values.get(i);
            if (isNullOrEmpty(value)) {
                continue;
            }
            writers.writeAttribute(i, rowId, value);
        }
    }

    private String generateRowId(List<String> headers, CSVRecord values, int pkIndex, int rowIndex) {
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

    private static boolean isNullOrEmpty(String value) {
        if (value == null) return true;
        var trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.equals("\\N");
    }

    private static CSVPrinter openPrinter(Path path) throws IOException {
        return OUTPUT_CSV.print(Files.newBufferedWriter(path));
    }

    private static String md5(String input) {
        try {
            var md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is unavailable", e);
        }
    }

    private static void closeAll(List<? extends Closeable> closeables) throws IOException {
        IOException failure = null;
        for (var closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private record TableLayout(List<String> headers, int primaryKeyIndex) {
    }

    private static final class OutputWriters implements AutoCloseable {
        private final CSVPrinter idWriter;
        private final List<CSVPrinter> attributeWritersByIndex;

        private OutputWriters(CSVPrinter idWriter, List<CSVPrinter> attributeWritersByIndex) {
            this.idWriter = idWriter;
            this.attributeWritersByIndex = attributeWritersByIndex;
        }

        static OutputWriters open(Path outputDir, String tableName, List<String> headers) throws IOException {
            var openedWriters = new ArrayList<Closeable>();
            try {
                var idWriter = openPrinter(outputDir.resolve(tableName + "__ID.csv"));
                openedWriters.add(idWriter);
                idWriter.printRecord("id");

                var attributeWriters = new ArrayList<CSVPrinter>(headers.size());
                for (var header : headers) {
                    var writer = openPrinter(outputDir.resolve(tableName + "_" + header + ".csv"));
                    openedWriters.add(writer);
                    writer.printRecord("id", "v");
                    attributeWriters.add(writer);
                }
                return new OutputWriters(idWriter, attributeWriters);
            } catch (IOException e) {
                try {
                    closeAll(openedWriters);
                } catch (IOException closeError) {
                    e.addSuppressed(closeError);
                }
                throw e;
            }
        }

        void writeId(String rowId) throws IOException {
            idWriter.printRecord(rowId);
        }

        void writeAttribute(int index, String rowId, String value) throws IOException {
            var writer = attributeWritersByIndex.get(index);
            writer.printRecord(rowId, value);
        }

        @Override
        public void close() throws IOException {
            var writers = new ArrayList<Closeable>(attributeWritersByIndex.size() + 1);
            writers.add(idWriter);
            writers.addAll(attributeWritersByIndex);
            closeAll(writers);
        }
    }
}
