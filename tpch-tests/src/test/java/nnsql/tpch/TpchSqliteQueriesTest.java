package nnsql.tpch;

import nnsql.tpch.framework.ResultSetComparator;
import nnsql.tpch.framework.TPCHQueryTest;
import nnsql.tpch.framework.TranslatedDbEnvironment;
import nnsql.tpch.framework.TranslatedDbExtension;
import nnsql.tpch.framework.TpchHtmlReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TranslatedDbExtension.class)
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TpchSqliteQueriesTest {
    private static final boolean LOG_QUERIES = Boolean.getBoolean("nnsql.tpch.logQueries");
    private static final boolean INCLUDE_QUERIES_IN_FAILURE =
        LOG_QUERIES || Boolean.getBoolean("nnsql.tpch.includeQueriesInFailure");
    private TranslatedDbEnvironment env;

    @BeforeAll
    void setUp(TranslatedDbEnvironment env) {
        this.env = env;
    }

    @TPCHQueryTest("h01.sql")
    void q01(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q01", query, orderSensitive);
    }

    @TPCHQueryTest("h02.sql")
    void q02(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q02", query, orderSensitive);
    }

    @TPCHQueryTest("h03.sql")
    void q03(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q03", query, orderSensitive);
    }

    @TPCHQueryTest("h04.sql")
    void q04(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q04", query, orderSensitive);
    }

    @TPCHQueryTest("h05.sql")
    void q05(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q05", query, orderSensitive);
    }

    @TPCHQueryTest("h06.sql")
    void q06(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q06", query, orderSensitive);
    }

    @TPCHQueryTest("h07.sql")
    void q07(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q07", query, orderSensitive);
    }

    @TPCHQueryTest("h08.sql")
    void q08(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q08", query, orderSensitive);
    }

    @TPCHQueryTest("h09.sql")
    void q09(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q09", query, orderSensitive);
    }

    @TPCHQueryTest("h10.sql")
    void q10(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q10", query, orderSensitive);
    }

    @TPCHQueryTest("h11.sql")
    void q11(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q11", query, orderSensitive);
    }

    @TPCHQueryTest("h12.sql")
    void q12(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q12", query, orderSensitive);
    }

    @TPCHQueryTest("h13.sql")
    void q13(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q13", query, orderSensitive);
    }

    @TPCHQueryTest("h14.sql")
    void q14(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q14", query, orderSensitive);
    }

    @TPCHQueryTest("h15.sql")
    void q15(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q15", query, orderSensitive);
    }

    @TPCHQueryTest("h16.sql")
    void q16(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q16", query, orderSensitive);
    }

    @TPCHQueryTest("h17.sql")
    void q17(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q17", query, orderSensitive);
    }

    @TPCHQueryTest("h18.sql")
    void q18(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q18", query, orderSensitive);
    }

    @TPCHQueryTest("h19.sql")
    void q19(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q19", query, orderSensitive);
    }

    @TPCHQueryTest("h20.sql")
    void q20(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q20", query, orderSensitive);
    }

    @TPCHQueryTest("h21.sql")
    void q21(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q21", query, orderSensitive);
    }

    @TPCHQueryTest("h22.sql")
    void q22(String query, boolean orderSensitive) throws Exception {
        assertTpchQuery("Q22", query, orderSensitive);
    }

    private void assertTpchQuery(
        String queryName,
        String query,
        boolean orderSensitive
    ) throws Exception {
        assertNotNull(env, "TranslatedDbEnvironment was not initialized");
        assertQueryAvailable(queryName, query);

        String translated = null;
        TranslatedDbEnvironment.QueryExecution sourceExecution = null;
        TranslatedDbEnvironment.QueryExecution translatedExecution = null;
        String failureMessage = null;
        boolean success = false;

        try {
            try {
                translated = env.translator().translate(query);
            } catch (Exception e) {
                failureMessage = "Translation failed for %s: %s".formatted(queryName, e.getMessage());
                throw new RuntimeException(failureMessage, e);
            }

            if (LOG_QUERIES) {
                System.out.println("==== " + queryName + " original query ====");
                System.out.println(query);
                System.out.println("==== " + queryName + " translated query ====");
                System.out.println(translated);
            }

            sourceExecution = env.source().executeWithDiagnostics(query);
            translatedExecution = env.target().executeWithDiagnostics(translated);

            ResultSetComparator.assertResultsMatch(
                sourceExecution.rows(),
                translatedExecution.rows(),
                queryName,
                orderSensitive
            );
            success = true;
        } catch (AssertionError e) {
            failureMessage = assertionWithQueries(e.getMessage(), query, translated);
            throw new AssertionError(failureMessage, e);
        } catch (Exception e) {
            if (failureMessage == null || failureMessage.isBlank()) {
                failureMessage = e.getMessage();
            }
            throw e;
        } finally {
            TpchHtmlReport.record(new TpchHtmlReport.QueryReportEntry(
                queryName,
                orderSensitive,
                query,
                sourceExecution == null ? null : sourceExecution.executionTimeMs(),
                sourceExecution == null ? null : sourceExecution.explainPlan(),
                sourceExecution == null ? null : sourceExecution.explainHtml(),
                sourceExecution == null ? null : sourceExecution.rows().size(),
                translated,
                translatedExecution == null ? null : translatedExecution.executionTimeMs(),
                translatedExecution == null ? null : translatedExecution.explainPlan(),
                translatedExecution == null ? null : translatedExecution.explainHtml(),
                translatedExecution == null ? null : translatedExecution.rows().size(),
                success,
                failureMessage
            ));
        }
    }

    private static void assertQueryAvailable(String queryName, String query) {
        assertNotNull(query, "Query must not be null for " + queryName);
        assertFalse(query.isBlank(), "Query must not be blank for " + queryName);
    }

    private static String assertionWithQueries(String message, String query, String translated) {
        if (!INCLUDE_QUERIES_IN_FAILURE) {
            return message;
        }
        var translatedSql = translated == null ? "(not available)" : translated;
        return message
            + System.lineSeparator()
            + "Original query:"
            + System.lineSeparator()
            + query
            + System.lineSeparator()
            + "Translated query:"
            + System.lineSeparator()
            + translatedSql;
    }
}
