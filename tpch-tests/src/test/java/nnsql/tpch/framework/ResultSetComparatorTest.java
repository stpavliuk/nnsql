package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultSetComparatorTest {

    @Test
    void comparesAsBagWhenOrderNotSensitive() {
        List<List<Object>> expected = List.of(List.of(1), List.of(2));
        List<List<Object>> actual = List.of(List.of(2), List.of(1));

        assertDoesNotThrow(() ->
            ResultSetComparator.assertResultsMatch(expected, actual, "Qx", false));
    }

    @Test
    void comparesInOrderWhenOrderSensitive() {
        List<List<Object>> expected = List.of(List.of(1), List.of(2));
        List<List<Object>> actual = List.of(List.of(2), List.of(1));

        assertThrows(AssertionError.class, () ->
            ResultSetComparator.assertResultsMatch(expected, actual, "Qx", true));
    }
}
