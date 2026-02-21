package nnsql.tpch;

import nnsql.tpch.framework.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

/**
 * End-to-end benchmark parity test:
 * native TPCH query results from DuckDB are compared with results from translated 6NF SQL.
 * Unit tests cover translation pieces; this class guards full pipeline behavior.
 */
@ExtendWith(TranslatedDbExtension.class)
@Tag("integration")
class TpchQueryTest {

    @TestFactory
    Stream<DynamicTest> tpchQueries(TranslatedDbEnvironment env) {
        return env.queries().stream().map(q ->
            DynamicTest.dynamicTest("TPC-H " + q.name(), () -> {
                String translated;
                try {
                    translated = env.translator().translate(q.sql());
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Translation failed for %s: %s".formatted(q.name(), e.getMessage()), e);
                }

                var expected = env.source().execute(q.sql());
                var actual = env.target().execute(translated);
                ResultSetComparator.assertResultsMatch(expected, actual, q.name(), q.orderSensitive());
            })
        );
    }
}
