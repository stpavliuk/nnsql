package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TpchNullPreprocessorTest {

    @Test
    void zeroNullRateLeavesRowsUnchanged() {
        var headers = List.of("a", "b");
        var rows = List.of(
            List.of("1", "x"),
            List.of("2", "y")
        );

        var mutated = TpchNullPreprocessor.nullifyTable("sample", headers, rows, Set.of(), 0.0d);

        assertEquals(rows, mutated);
    }

    @Test
    void nonZeroNullRateKeepsAtLeastOneNonNullPerEligibleColumn() {
        var headers = List.of("id", "a", "b");
        var rows = List.of(
            List.of("1", "x1", "y1"),
            List.of("2", "x2", "y2"),
            List.of("3", "x3", "y3")
        );

        var mutated = TpchNullPreprocessor.nullifyTable("sample", headers, rows, Set.of("id"), 0.7d);

        assertEquals(List.of("1", "2", "3"), mutated.stream().map(row -> row.getFirst()).toList());
        assertTrue(mutated.stream().anyMatch(row -> !row.get(1).isBlank()));
        assertTrue(mutated.stream().anyMatch(row -> !row.get(2).isBlank()));
        assertTrue(mutated.stream().flatMap(List::stream).anyMatch(String::isBlank));
    }

    @Test
    void nonZeroNullRatePreservesPrimaryKeys() {
        var headers = List.of("id", "value");
        var rows = List.of(
            List.of("1", "x1"),
            List.of("2", "x2"),
            List.of("3", "x3")
        );

        var mutated = TpchNullPreprocessor.nullifyTable("sample", headers, rows, Set.of("id"), 0.9d);

        assertEquals(List.of("1", "2", "3"), mutated.stream().map(row -> row.getFirst()).toList());
    }
}
