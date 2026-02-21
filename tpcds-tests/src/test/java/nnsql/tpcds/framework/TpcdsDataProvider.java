package nnsql.tpcds.framework;

import java.sql.*;
import java.util.*;

public class TpcdsDataProvider implements BenchmarkDataProvider {

    private static final String QUERIES_PROPERTY = "nnsql.tpcds.queries";

    private final double scaleFactor;

    public TpcdsDataProvider(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public void generate(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("INSTALL tpcds");
            stmt.execute("LOAD tpcds");
            stmt.execute("CALL dsdgen(sf=%s, keys=true)".formatted(scaleFactor));
        }
    }

    @Override
    public List<BenchmarkQuery> queries(Connection conn) throws SQLException {
        var filter = parseQueryFilter();
        var queries = new ArrayList<BenchmarkQuery>();
        try (var rs = conn.createStatement().executeQuery("SELECT query_nr, query FROM tpcds_queries()")) {
            while (rs.next()) {
                var queryNr = rs.getInt("query_nr");
                if (!filter.isEmpty() && !filter.contains(queryNr)) {
                    continue;
                }
                var sql = rs.getString("query");
                var cleaned = normalizeQuerySql(sql);
                if (cleaned != null) {
                    queries.add(new BenchmarkQuery(
                        "Q" + queryNr,
                        cleaned,
                        hasOuterOrderBy(cleaned)
                    ));
                }
            }
        }
        return queries;
    }

    static Set<Integer> parseQueryFilter() {
        var prop = System.getProperty(QUERIES_PROPERTY);
        if (prop == null || prop.isBlank()) {
            return Set.of();
        }
        var result = new HashSet<Integer>();
        for (var part : prop.split(",")) {
            var trimmed = part.strip();
            if (trimmed.isEmpty()) continue;

            int dashIdx = trimmed.indexOf('-');
            if (dashIdx > 0 && dashIdx < trimmed.length() - 1) {
                int start = Integer.parseInt(trimmed.substring(0, dashIdx).strip());
                int end = Integer.parseInt(trimmed.substring(dashIdx + 1).strip());
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            } else {
                result.add(Integer.parseInt(trimmed));
            }
        }
        return result;
    }

    static String normalizeQuerySql(String sql) {
        if (sql == null || sql.isBlank()) return null;
        var cleaned = sql.strip();
        if (cleaned.endsWith(";")) cleaned = cleaned.substring(0, cleaned.length() - 1).strip();
        return cleaned;
    }

    static boolean hasOuterOrderBy(String sql) {
        if (sql == null || sql.isBlank()) return false;
        return findOuterKeyword(sql, "ORDER BY") >= 0;
    }

    static int findOuterKeyword(String sql, String keyword) {
        int depth = 0;
        var upper = sql.toUpperCase();
        var kw = keyword.toUpperCase();

        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && upper.startsWith(kw, i)) {
                if (i == 0 || !Character.isLetterOrDigit(upper.charAt(i - 1))) {
                    int end = i + kw.length();
                    if (end >= upper.length() || !Character.isLetterOrDigit(upper.charAt(end))) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}
