package nnsql.tpch.framework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TpchDataProviderTest {
    private static final String QUERIES_PROPERTY = "nnsql.tpch.queries";

    @AfterEach
    void clearFilter() {
        System.clearProperty(QUERIES_PROPERTY);
    }

    @Test
    void normalizeQuerySqlPreservesOrderByAndLimit() {
        var sql = TpchDataProvider.normalizeQuerySql("SELECT a FROM t ORDER BY a LIMIT 10;");
        assertEquals("SELECT a FROM t ORDER BY a LIMIT 10", sql);
    }

    @Test
    void hasOuterOrderByIgnoresNestedOrderBy() {
        assertTrue(TpchDataProvider.hasOuterOrderBy(
            "SELECT a FROM t ORDER BY a LIMIT 10"));

        assertFalse(TpchDataProvider.hasOuterOrderBy(
            "SELECT * FROM (SELECT a FROM t ORDER BY a) x"));
    }

    @Test
    void queriesLoadsAll22ByDefault() throws Exception {
        var provider = new TpchDataProvider(0.01);
        var queries = provider.queries(null);

        assertEquals(22, queries.size());
        assertEquals("Q01", queries.getFirst().name());
        assertEquals("Q22", queries.getLast().name());
    }

    @Test
    void queriesFilterSupportsIdsAndRanges() throws Exception {
        System.setProperty(QUERIES_PROPERTY, "1,3-4,22");
        var provider = new TpchDataProvider(0.01);

        var queries = provider.queries(null);

        assertEquals(4, queries.size());
        assertEquals("Q01", queries.getFirst().name());
        assertEquals("Q03", queries.get(1).name());
        assertEquals("Q04", queries.get(2).name());
        assertEquals("Q22", queries.getLast().name());
    }
}
