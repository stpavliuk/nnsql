package nnsql.tpch.framework;

import nnsql.query.QueryTranslator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class TranslatedDbEnvironment implements AutoCloseable {

    private static final int QUERY_TIMEOUT_SECONDS = 15;

    private final Connection sourceConn;
    private final Connection targetConn;
    private final QueryTranslator queryTranslator;
    private final List<BenchmarkDataProvider.BenchmarkQuery> benchmarkQueries;
    private final Path tempDir;
    private final boolean cleanupOnClose;

    TranslatedDbEnvironment(
        Connection sourceConn,
        Connection targetConn,
        QueryTranslator queryTranslator,
        List<BenchmarkDataProvider.BenchmarkQuery> benchmarkQueries,
        Path tempDir,
        boolean cleanupOnClose
    ) {
        this.sourceConn = sourceConn;
        this.targetConn = targetConn;
        this.queryTranslator = queryTranslator;
        this.benchmarkQueries = benchmarkQueries;
        this.tempDir = tempDir;
        this.cleanupOnClose = cleanupOnClose;
    }

    public DatabaseHandle source() {
        return new DatabaseHandle(sourceConn, QUERY_TIMEOUT_SECONDS);
    }

    public DatabaseHandle target() {
        return new DatabaseHandle(targetConn, QUERY_TIMEOUT_SECONDS);
    }

    public QueryTranslator translator() {
        return queryTranslator;
    }

    public List<BenchmarkDataProvider.BenchmarkQuery> queries() {
        return benchmarkQueries;
    }

    @Override
    public void close() {
        closeQuietly(sourceConn);
        closeQuietly(targetConn);

        if (!cleanupOnClose || tempDir == null) {
            return;
        }

        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (Exception _) {
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to cleanup db: %s".formatted(e));
        }
    }

    public record DatabaseHandle(Connection connection, int queryTimeoutSeconds) {
        public QueryExecution executeWithDiagnostics(String sql) throws SQLException {
            var explainPlan = explain(sql);
            var explainHtml = explainHtml(sql);
            var startedAtNanos = System.nanoTime();
            var rows = execute(sql);
            var elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000.0;
            return new QueryExecution(rows, elapsedMs, explainPlan, explainHtml);
        }

        public List<List<Object>> execute(String sql) throws SQLException {
            try (var stmt = connection.createStatement()) {
                stmt.setQueryTimeout(queryTimeoutSeconds);
                var rs = stmt.executeQuery(sql);
                var meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                var rows = new ArrayList<List<Object>>();
                while (rs.next()) {
                    var row = new ArrayList<Object>(colCount);
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }

        private String explain(String sql) throws SQLException {
            try (var stmt = connection.createStatement()) {
                stmt.setQueryTimeout(queryTimeoutSeconds);
                try (var rs = stmt.executeQuery("EXPLAIN " + sql)) {
                    return readExplainRows(rs);
                }
            }
        }

        private String explainHtml(String sql) {
            try (var stmt = connection.createStatement()) {
                stmt.setQueryTimeout(queryTimeoutSeconds);
                try (var rs = stmt.executeQuery("EXPLAIN (FORMAT html) " + sql)) {
                    return readExplainHtmlRows(rs);
                }
            } catch (SQLException _) {
                return null;
            }
        }

        private static String readExplainRows(ResultSet rs) throws SQLException {
            var rows = new ArrayList<String>();
            var meta = rs.getMetaData();
            var columnCount = meta.getColumnCount();

            while (rs.next()) {
                var columns = new ArrayList<String>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    var value = rs.getObject(i);
                    columns.add(value == null ? "NULL" : value.toString());
                }
                rows.add(String.join(" | ", columns));
            }

            if (rows.isEmpty()) {
                return "(no explain output)";
            }

            return String.join(System.lineSeparator(), rows);
        }

        private static String readExplainHtmlRows(ResultSet rs) throws SQLException {
            var meta = rs.getMetaData();
            var columnCount = meta.getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    var value = rs.getObject(i);
                    if (value == null) {
                        continue;
                    }

                    var htmlDocument = extractHtmlDocument(value.toString());
                    if (htmlDocument != null) {
                        return htmlDocument;
                    }
                }
            }

            return null;
        }

        private static String extractHtmlDocument(String text) {
            if (text == null || text.isBlank()) {
                return null;
            }

            var lower = text.toLowerCase(Locale.ROOT);
            var doctypeIndex = lower.indexOf("<!doctype html>");
            if (doctypeIndex >= 0) {
                return text.substring(doctypeIndex).trim();
            }

            var htmlIndex = lower.indexOf("<html");
            if (htmlIndex >= 0) {
                return text.substring(htmlIndex).trim();
            }

            return null;
        }
    }

    public record QueryExecution(
        List<List<Object>> rows,
        double executionTimeMs,
        String explainPlan,
        String explainHtml
    ) {}

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception _) {
        }
    }
}
