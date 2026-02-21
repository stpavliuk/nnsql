package nnsql.tpch;

import nnsql.tpch.framework.TPCHQueryTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpchSqliteQueriesTest {
    @TPCHQueryTest("h01.sql")
    void q01(String query) {
        assertQueryAvailable("h01.sql", query);
    }

    @TPCHQueryTest("h02.sql")
    void q02(String query) {
        assertQueryAvailable("h02.sql", query);
    }

    @TPCHQueryTest("h03.sql")
    void q03(String query) {
        assertQueryAvailable("h03.sql", query);
    }

    @TPCHQueryTest("h04.sql")
    void q04(String query) {
        assertQueryAvailable("h04.sql", query);
    }

    @TPCHQueryTest("h05.sql")
    void q05(String query) {
        assertQueryAvailable("h05.sql", query);
    }

    @TPCHQueryTest("h06.sql")
    void q06(String query) {
        assertQueryAvailable("h06.sql", query);
    }

    @TPCHQueryTest("h07.sql")
    void q07(String query) {
        assertQueryAvailable("h07.sql", query);
    }

    @TPCHQueryTest("h08.sql")
    void q08(String query) {
        assertQueryAvailable("h08.sql", query);
    }

    @TPCHQueryTest("h09.sql")
    void q09(String query) {
        assertQueryAvailable("h09.sql", query);
    }

    @TPCHQueryTest("h10.sql")
    void q10(String query) {
        assertQueryAvailable("h10.sql", query);
    }

    @TPCHQueryTest("h11.sql")
    void q11(String query) {
        assertQueryAvailable("h11.sql", query);
    }

    @TPCHQueryTest("h12.sql")
    void q12(String query) {
        assertQueryAvailable("h12.sql", query);
    }

    @TPCHQueryTest("h13.sql")
    void q13(String query) {
        assertQueryAvailable("h13.sql", query);
    }

    @TPCHQueryTest("h14.sql")
    void q14(String query) {
        assertQueryAvailable("h14.sql", query);
    }

    @TPCHQueryTest("h15.sql")
    void q15(String query) {
        assertQueryAvailable("h15.sql", query);
    }

    @TPCHQueryTest("h16.sql")
    void q16(String query) {
        assertQueryAvailable("h16.sql", query);
    }

    @TPCHQueryTest("h17.sql")
    void q17(String query) {
        assertQueryAvailable("h17.sql", query);
    }

    @TPCHQueryTest("h18.sql")
    void q18(String query) {
        assertQueryAvailable("h18.sql", query);
    }

    @TPCHQueryTest("h19.sql")
    void q19(String query) {
        assertQueryAvailable("h19.sql", query);
    }

    @TPCHQueryTest("h20.sql")
    void q20(String query) {
        assertQueryAvailable("h20.sql", query);
    }

    @TPCHQueryTest("h21.sql")
    void q21(String query) {
        assertQueryAvailable("h21.sql", query);
    }

    @TPCHQueryTest("h22.sql")
    void q22(String query) {
        assertQueryAvailable("h22.sql", query);
    }

    private static void assertQueryAvailable(String fileName, String query) {
        assertNotNull(query, "Query must not be null for " + fileName);
        assertFalse(query.isBlank(), "Query must not be blank for " + fileName);
        assertTrue(query.regionMatches(true, 0, "SELECT", 0, "SELECT".length()),
            "Expected SELECT query in " + fileName);
    }
}
