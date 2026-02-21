package nnsql.tpch.framework;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ResultSetComparator {

    private static final double FLOAT_TOLERANCE = 1e-6;

    public static void assertResultsMatch(
        List<List<Object>> expected,
        List<List<Object>> actual,
        String queryName
    ) {
        assertResultsMatch(expected, actual, queryName, false);
    }

    public static void assertResultsMatch(
        List<List<Object>> expected,
        List<List<Object>> actual,
        String queryName,
        boolean orderSensitive
    ) {
        assertEquals(expected.size(), actual.size(),
            "%s: row count mismatch (expected %d, got %d)".formatted(queryName, expected.size(), actual.size()));

        if (expected.isEmpty()) return;

        assertEquals(expected.getFirst().size(), actual.getFirst().size(),
            "%s: column count mismatch".formatted(queryName));

        var comparedExpected = orderSensitive ? expected : sortRows(expected);
        var comparedActual = orderSensitive ? actual : sortRows(actual);

        for (int i = 0; i < comparedExpected.size(); i++) {
            var expRow = comparedExpected.get(i);
            var actRow = comparedActual.get(i);
            for (int j = 0; j < expRow.size(); j++) {
                assertTrue(valuesEqual(expRow.get(j), actRow.get(j)),
                    "%s: mismatch at row %d, col %d: expected <%s> but got <%s>"
                        .formatted(queryName, i, j, expRow.get(j), actRow.get(j)));
            }
        }
    }

    private static List<List<Object>> sortRows(List<List<Object>> rows) {
        var sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int cmp = compareValues(a.get(i), b.get(i));
                if (cmp != 0) return cmp;
            }
            return Integer.compare(a.size(), b.size());
        });
        return sorted;
    }

    private static int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }

        if (a instanceof Comparable<?> ca && b instanceof Comparable<?>) {
            return compareComparableOrFallback(ca, b, a, b);
        }

        return a.toString().compareTo(b.toString());
    }

    private static int compareComparableOrFallback(
        Comparable<?> left,
        Object right,
        Object fallbackLeft,
        Object fallbackRight
    ) {
        try {
            return compareTyped(left, right);
        } catch (ClassCastException _) {
            return fallbackLeft.toString().compareTo(fallbackRight.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<? super T>> int compareTyped(Comparable<?> left, Object right) {
        return ((T) left).compareTo((T) right);
    }

    private static boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;

        if (a instanceof Number na && b instanceof Number nb) {
            if (isFloatingPoint(na) || isFloatingPoint(nb)) {
                return Math.abs(na.doubleValue() - nb.doubleValue()) < FLOAT_TOLERANCE;
            }
            return toBigDecimal(na).compareTo(toBigDecimal(nb)) == 0;
        }

        return Objects.equals(a, b);
    }

    private static boolean isFloatingPoint(Number n) {
        return switch (n) {
            case Float _, Double _ -> true;
            default -> false;
        };
    }

    private static BigDecimal toBigDecimal(Number n) {
        return switch (n) {
            case BigDecimal bd -> bd;
            case Long l -> BigDecimal.valueOf(l);
            case Integer i -> BigDecimal.valueOf(i);
            default -> new BigDecimal(n.toString());
        };
    }
}
