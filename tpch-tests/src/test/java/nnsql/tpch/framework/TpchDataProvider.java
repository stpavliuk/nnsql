package nnsql.tpch.framework;

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
        if (sql == null || sql.isBlank()) return null;
        var withoutComments = sql.lines()
            .map(TpchDataProvider::stripLineComment)
            .toList();
        var cleaned = String.join("\n", withoutComments).strip();
        if (cleaned.endsWith(";")) cleaned = cleaned.substring(0, cleaned.length() - 1).strip();
        return cleaned;
    }

    static boolean hasOuterOrderBy(String sql) {
        if (sql == null || sql.isBlank()) return false;
        return findOuterKeyword(sql, "ORDER BY") >= 0;
    }

    static int findOuterKeyword(String sql, String keyword) {
        var upper = sql.toUpperCase();
        var kw = keyword.toUpperCase();
        int depth = 0;

        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c == '(') {
                depth++;
                continue;
            }
            if (c == ')') {
                depth--;
                continue;
            }
            if (depth == 0 && matchesKeyword(upper, kw, i)) {
                return i;
            }
        }
        return -1;
    }

    private static String stripLineComment(String line) {
        var idx = line.indexOf("--");
        return idx >= 0 ? line.substring(0, idx) : line;
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

    private static boolean matchesKeyword(String sql, String keyword, int position) {
        if (!sql.startsWith(keyword, position)) {
            return false;
        }
        if (position > 0 && Character.isLetterOrDigit(sql.charAt(position - 1))) {
            return false;
        }
        var end = position + keyword.length();
        return end >= sql.length() || !Character.isLetterOrDigit(sql.charAt(end));
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
