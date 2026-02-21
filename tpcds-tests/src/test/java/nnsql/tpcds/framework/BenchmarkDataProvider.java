package nnsql.tpcds.framework;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface BenchmarkDataProvider {
    void generate(Connection conn) throws SQLException;
    List<BenchmarkQuery> queries(Connection conn) throws SQLException;

    record BenchmarkQuery(String name, String sql, boolean orderSensitive) {}
}
