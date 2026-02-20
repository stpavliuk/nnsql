package nnsql.tpcds.framework;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TpcdsDataProvider implements BenchmarkDataProvider {
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
        var queries = new ArrayList<BenchmarkQuery>();
        try (var rs = conn.createStatement().executeQuery("SELECT query_nr, query FROM tpcds_queries()")) {
            while (rs.next()) {
                var queryNr = rs.getInt("query_nr");
                var sql = rs.getString("query");
                var cleaned = stripOrderByAndLimit(sql);
                if (cleaned != null) {
                    queries.add(new BenchmarkQuery("Q" + queryNr, cleaned));
                }
            }
        }
        return queries;
    }

    static String stripOrderByAndLimit(String sql) {
        if (sql == null || sql.isBlank()) return null;
        var cleaned = sql.strip();
        if (cleaned.endsWith(";")) cleaned = cleaned.substring(0, cleaned.length() - 1).strip();

        int outerOrderBy = findOuterKeyword(cleaned, "ORDER BY");
        if (outerOrderBy >= 0) {
            cleaned = cleaned.substring(0, outerOrderBy).strip();
        }

        int outerLimit = findOuterKeyword(cleaned, "LIMIT");
        if (outerLimit >= 0) {
            cleaned = cleaned.substring(0, outerLimit).strip();
        }

        return cleaned;
    }

    private static int findOuterKeyword(String sql, String keyword) {
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
