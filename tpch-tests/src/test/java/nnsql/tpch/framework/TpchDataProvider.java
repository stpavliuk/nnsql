package nnsql.tpch.framework;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.IntStream;

public class TpchDataProvider implements BenchmarkDataProvider {

    private static final String QUERIES_PROPERTY = "nnsql.tpch.queries";
    private static final String RESOURCE_ROOT = "tpch/sqlite_tpc/";
    private static final int QUERY_COUNT = 22;

    private final double scaleFactor;

    public TpchDataProvider(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public void generate(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("INSTALL tpch");
            stmt.execute("LOAD tpch");
            stmt.execute("CALL dbgen(sf=%s)".formatted(scaleFactor));
        }
    }

    @Override
    public List<BenchmarkQuery> queries(Connection conn) {
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
        var dashIdx = token.indexOf('-');
        if (dashIdx > 0 && dashIdx < token.length() - 1) {
            var start = Integer.parseInt(token.substring(0, dashIdx).strip());
            var end = Integer.parseInt(token.substring(dashIdx + 1).strip());
            return IntStream.rangeClosed(start, end);
        }
        return IntStream.of(Integer.parseInt(token));
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
