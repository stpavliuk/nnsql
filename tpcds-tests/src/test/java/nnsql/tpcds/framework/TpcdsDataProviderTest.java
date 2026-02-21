package nnsql.tpcds.framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TpcdsDataProviderTest {

    @Test
    void normalizeQuerySqlPreservesOrderByAndLimit() {
        var sql = TpcdsDataProvider.normalizeQuerySql("SELECT a FROM t ORDER BY a LIMIT 10;");
        assertEquals("SELECT a FROM t ORDER BY a LIMIT 10", sql);
    }

    @Test
    void hasOuterOrderByIgnoresNestedOrderBy() {
        assertTrue(TpcdsDataProvider.hasOuterOrderBy(
            "SELECT a FROM t ORDER BY a LIMIT 10"));

        assertFalse(TpcdsDataProvider.hasOuterOrderBy(
            "SELECT * FROM (SELECT a FROM t ORDER BY a) x"));
    }
}
