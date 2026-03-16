package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslatedDbEnvironmentTest {

    @Test
    void executeWithDiagnosticsAveragesAcrossConfiguredRuns() throws Exception {
        var originalTimingRuns = System.getProperty("nnsql.tpch.timingRuns");
        var originalWarmupRuns = System.getProperty("nnsql.tpch.timingWarmupRuns");
        System.setProperty("nnsql.tpch.timingRuns", "3");
        System.setProperty("nnsql.tpch.timingWarmupRuns", "1");

        try (var connection = DriverManager.getConnection("jdbc:duckdb:")) {
            try (var stmt = connection.createStatement()) {
                stmt.execute("create sequence timing_seq start 1");
            }

            var handle = new TranslatedDbEnvironment.DatabaseHandle(connection, 15);
            var execution = handle.executeWithDiagnostics("select nextval('timing_seq')");

            assertEquals(1, execution.rows().size());
            assertEquals(4L, ((Number) execution.rows().getFirst().getFirst()).longValue());
            assertTrue(execution.executionTimeMs() >= 0.0d);

            try (var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("select currval('timing_seq')")) {
                assertTrue(rs.next());
                assertEquals(4L, rs.getLong(1));
            }
        } finally {
            restoreProperty("nnsql.tpch.timingRuns", originalTimingRuns);
            restoreProperty("nnsql.tpch.timingWarmupRuns", originalWarmupRuns);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }
}
