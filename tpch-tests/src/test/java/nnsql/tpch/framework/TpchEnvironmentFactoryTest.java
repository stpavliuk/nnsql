package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpchEnvironmentFactoryTest {

    @Test
    void refreshOptimizerStatisticsExecutesAnalyze() throws Exception {
        var executedSql = new ArrayList<String>();
        var statementClosed = new AtomicBoolean(false);
        var statement = statementProxy(executedSql, statementClosed);
        var connection = connectionProxy(statement);

        TpchEnvironmentFactory.refreshOptimizerStatistics(connection);

        assertEquals(List.of("ANALYZE"), executedSql);
        assertTrue(statementClosed.get());
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

    private static Statement statementProxy(List<String> executedSql, AtomicBoolean statementClosed) {
        return (Statement) Proxy.newProxyInstance(
            Statement.class.getClassLoader(),
            new Class<?>[] {Statement.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "execute" -> {
                    executedSql.add((String) args[0]);
                    yield true;
                }
                case "close" -> {
                    statementClosed.set(true);
                    yield null;
                }
                case "isClosed" -> statementClosed.get();
                case "isWrapperFor" -> false;
                case "unwrap" -> null;
                case "toString" -> "statement-proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );
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
