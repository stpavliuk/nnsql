package nnsql.tpch.framework;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class TpchDataProvider implements BenchmarkDataProvider {

    private static final String QUERIES_PROPERTY = "nnsql.tpch.queries";
    private static final String RESOURCE_ROOT = "tpch/queries/";
    private static final int QUERY_COUNT = 22;

    @Override
    public List<BenchmarkQuery> queries() {
        var filter = parseQueryFilter();
        return IntStream.rangeClosed(1, QUERY_COUNT)
            .filter(queryNr -> filter.isEmpty() || filter.contains(queryNr))
            .mapToObj(TpchDataProvider::loadQuery)
            .flatMap(Optional::stream)
            .toList();
    }

    static Set<Integer> parseQueryFilter() {
        var prop = System.getProperty(QUERIES_PROPERTY);
        if (prop == null || prop.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(prop.split(","))
            .map(String::strip)
            .filter(part -> !part.isEmpty())
            .flatMapToInt(TpchDataProvider::expandRange)
            .boxed()
            .collect(java.util.stream.Collectors.toSet());
    }

    static String normalizeQuerySql(String sql) {
        if (sql == null || sql.isBlank()) {
            return null;
        }
        return parseTopLevelSelect(sql).toString();
    }

    static boolean hasOuterOrderBy(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        var parsed = parseTopLevelSelect(sql);
        return parsed.getOrderByElements() != null && !parsed.getOrderByElements().isEmpty();
    }

    private static IntStream expandRange(String token) {
        var normalizedToken = normalizeQueryToken(token);
        var dashIdx = token.indexOf('-');
        if (dashIdx > 0 && dashIdx < token.length() - 1) {
            var start = Integer.parseInt(normalizeQueryToken(token.substring(0, dashIdx)));
            var end = Integer.parseInt(normalizeQueryToken(token.substring(dashIdx + 1)));
            return IntStream.rangeClosed(start, end);
        }
        return IntStream.of(Integer.parseInt(normalizedToken));
    }

    private static String normalizeQueryToken(String token) {
        var stripped = token.strip();
        if (stripped.length() >= 2 && (stripped.charAt(0) == 'Q' || stripped.charAt(0) == 'q')) {
            return stripped.substring(1);
        }
        return stripped;
    }

    private static PlainSelect parseTopLevelSelect(String sql) {
        try {
            return (PlainSelect) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse TPCH SQL query", e);
        }
    }

    private static String readQueryResource(int queryNr) {
        var fileName = "h%02d.sql".formatted(queryNr);
        var resource = RESOURCE_ROOT + fileName;
        try (var in = TpchDataProvider.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing TPCH SQL resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read TPCH SQL resource: " + resource, e);
        }
    }

    private static Optional<BenchmarkQuery> loadQuery(int queryNr) {
        var cleaned = normalizeQuerySql(readQueryResource(queryNr));
        if (cleaned == null) {
            return Optional.empty();
        }
        return Optional.of(new BenchmarkQuery(
            "Q%02d".formatted(queryNr),
            cleaned,
            hasOuterOrderBy(cleaned)
        ));
    }
}
