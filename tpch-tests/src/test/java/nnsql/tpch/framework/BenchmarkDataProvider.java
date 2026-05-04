package nnsql.tpch.framework;

import java.util.List;

public interface BenchmarkDataProvider {
    List<BenchmarkQuery> queries();

    record BenchmarkQuery(String name, String sql, boolean orderSensitive) {}
}
