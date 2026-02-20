package nnsql.tpcds;

import nnsql.tpcds.framework.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

@ExtendWith(TranslatedDbExtension.class)
class TpcdsQueryTest {

    @TestFactory
    Stream<DynamicTest> tpcdsQueries(TranslatedDbEnvironment env) {
        return env.queries().stream().map(q ->
            DynamicTest.dynamicTest("TPC-DS " + q.name(), () -> {
                String translated;
                try {
                    translated = env.translator().translate(q.sql());
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Translation failed for %s: %s".formatted(q.name(), e.getMessage()), e);
                }

                var expected = env.source().execute(q.sql());
                var actual = env.target().execute(translated);
                ResultSetComparator.assertResultsMatch(expected, actual, q.name());
            })
        );
    }
}
