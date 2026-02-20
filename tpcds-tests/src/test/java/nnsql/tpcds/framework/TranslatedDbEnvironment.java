package nnsql.tpcds.framework;

import nnsql.query.QueryTranslator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class TranslatedDbEnvironment implements AutoCloseable {

    private static final int QUERY_TIMEOUT_SECONDS = 30;

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
        runSilently(() -> sourceConn.close());
        runSilently(() -> targetConn.close());

        if (!cleanupOnClose || tempDir == null) {
            return;
        }

        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (Exception _) {
                    System.out.println("");
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to cleanup db: %s".formatted(e));
        }
    }

    public record DatabaseHandle(Connection connection, int queryTimeoutSeconds) {
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
    }

    interface SilentRunner {
        void run() throws Exception;
    }

    private static void runSilently(SilentRunner r) {
        try {
            r.run();
        } catch (Exception e) {
        }
    }
}
