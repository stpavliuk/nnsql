package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpchQueryDiagnosticsTest {
    @Test
    void translatedWithParserExtractsCtesWithNestedParenthesesAndCommas() {
        var parsed = TpchQueryDiagnostics.TranslatedWithQuery.parse("""
            WITH first_cte AS (SELECT concat('a,b', x) AS v FROM r),
            second_cte AS (SELECT * FROM first_cte WHERE v IN (SELECT v FROM s))
            SELECT * FROM second_cte;
            """);

        assertEquals(2, parsed.ctes().size());
        assertEquals("first_cte", parsed.ctes().getFirst().name());
        assertEquals("SELECT concat('a,b', x) AS v FROM r", parsed.ctes().getFirst().body());
        assertEquals("second_cte", parsed.ctes().getLast().name());
        assertEquals(
            "SELECT * FROM first_cte WHERE v IN (SELECT v FROM s)",
            parsed.ctes().getLast().body()
        );
    }

    @Test
    void translatedWithParserExtractsMaterializedCtes() {
        var parsed = TpchQueryDiagnostics.TranslatedWithQuery.parse("""
            WITH first_cte AS MATERIALIZED (SELECT x FROM r)
            SELECT * FROM first_cte;
            """);

        assertEquals(1, parsed.ctes().size());
        assertEquals("first_cte", parsed.ctes().getFirst().name());
        assertEquals("SELECT x FROM r", parsed.ctes().getFirst().body());
    }
}
