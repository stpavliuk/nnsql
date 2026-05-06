package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslatedDbEnvironmentTest {

    @Test
    void executeWithDiagnosticsAveragesAcrossConfiguredRuns() throws Exception {
        var originalTimingRuns = System.getProperty("nnsql.tpch.timingRuns");
        var originalWarmupRuns = System.getProperty("nnsql.tpch.timingWarmupRuns");
        System.setProperty("nnsql.tpch.timingRuns", "3");
        System.setProperty("nnsql.tpch.timingWarmupRuns", "1");

        try {
            var queryExecutions = new AtomicInteger();
            var statement = statementProxy(queryExecutions);
            var connection = connectionProxy(statement);

            var handle = new TranslatedDbEnvironment.DatabaseHandle(connection, 15);
            var execution = handle.executeWithDiagnostics("select nextval('timing_seq')");

            assertEquals(1, execution.rows().size());
            assertEquals(4L, ((Number) execution.rows().getFirst().getFirst()).longValue());
            assertTrue(execution.executionTimeMs() >= 0.0d);
            assertEquals("QUERY PLAN", execution.explainPlan());
            assertEquals(4, queryExecutions.get());
        } finally {
            restoreProperty("nnsql.tpch.timingRuns", originalTimingRuns);
            restoreProperty("nnsql.tpch.timingWarmupRuns", originalWarmupRuns);
        }
    }

    private static Connection connectionProxy(Statement statement) {
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "createStatement" -> statement;
                case "close" -> null;
                case "isClosed", "isWrapperFor" -> false;
                case "unwrap" -> null;
                case "toString" -> "connection-proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Statement statementProxy(AtomicInteger queryExecutions) {
        return (Statement) Proxy.newProxyInstance(
            Statement.class.getClassLoader(),
            new Class<?>[] {Statement.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "setQueryTimeout", "close" -> null;
                case "executeQuery" -> executeQuery((String) args[0], queryExecutions);
                case "isClosed", "isWrapperFor" -> false;
                case "unwrap" -> null;
                case "toString" -> "statement-proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static ResultSet executeQuery(String sql, AtomicInteger queryExecutions) throws SQLException {
        if (sql.startsWith("EXPLAIN ")) {
            return resultSet(List.of(List.of("QUERY PLAN")));
        }

        var nextValue = queryExecutions.incrementAndGet();
        return resultSet(List.of(List.of((long) nextValue)));
    }

    private static ResultSet resultSet(List<List<Object>> rows) {
        var rowIndex = new AtomicInteger(-1);
        var metadata = metadataProxy(rows.isEmpty() ? 1 : rows.getFirst().size());

        return (ResultSet) Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "next" -> rowIndex.incrementAndGet() < rows.size();
                case "getObject" -> rows.get(rowIndex.get()).get(((Integer) args[0]) - 1);
                case "getMetaData" -> metadata;
                case "close" -> null;
                case "isClosed", "isWrapperFor" -> false;
                case "unwrap" -> null;
                case "toString" -> "result-set-proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static ResultSetMetaData metadataProxy(int columnCount) {
        return (ResultSetMetaData) Proxy.newProxyInstance(
            ResultSetMetaData.class.getClassLoader(),
            new Class<?>[] {ResultSetMetaData.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getColumnCount" -> columnCount;
                case "isWrapperFor" -> false;
                case "unwrap" -> null;
                case "toString" -> "metadata-proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        if (returnType == double.class) {
            return 0.0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        throw new IllegalArgumentException("Unsupported primitive type: " + returnType);
    }
}
