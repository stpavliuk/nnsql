package nnsql.tpch.framework;

public final class TpchQueryDebugger {
    private static final String DEFAULT_SCALE_FACTOR = "1";
    private static final double DEFAULT_NULL_RATE = 0.7d;
    private static final String JDBC_URL_PROPERTY = "nnsql.tpch.jdbcUrl";
    private static final String JDBC_USER_PROPERTY = "nnsql.tpch.jdbcUser";
    private static final String JDBC_PASSWORD_PROPERTY = "nnsql.tpch.jdbcPassword";
    private static final String SCALE_FACTOR_PROPERTY = "nnsql.tpch.scaleFactor";
    private static final String NULL_RATE_PROPERTY = "nnsql.tpch.nullRate";

    private TpchQueryDebugger() {
    }

    public static void main(String[] args) throws Exception {
        var provider = new TpchDataProvider();
        var scaleFactor = configuredOrDefault(SCALE_FACTOR_PROPERTY, DEFAULT_SCALE_FACTOR);
        var nullRate = Double.parseDouble(configuredOrDefault(
            NULL_RATE_PROPERTY,
            Double.toString(DEFAULT_NULL_RATE)
        ));
        var fixtureSet = TpchFixtureSet.load(scaleFactor);
        var cacheKey = TpchFixtureSet.cacheKey(nullRate, fixtureSet);
        var sourceSchema = "tpch_src_" + cacheKey;
        var targetSchema = "tpch_tgt_" + cacheKey;

        try (var env = TpchEnvironmentFactory.openOrBuildEnvironment(
            resolveConnectionConfig(),
            sourceSchema,
            targetSchema,
            fixtureSet,
            nullRate,
            provider
        )) {
            for (var query : env.queries()) {
                System.out.println(debug(query, env));
            }
        }
    }

    private static String debug(BenchmarkDataProvider.BenchmarkQuery query, TranslatedDbEnvironment env) throws Exception {
        var translatedSql = env.translator().translate(query.sql());
        var sourceExecution = env.source().executeWithDiagnostics(query.sql());
        var translatedExecution = env.target().executeWithDiagnostics(translatedSql);
        var diagnostics = TpchQueryDiagnostics.collect(
            query.name(),
            query.sql(),
            translatedSql,
            sourceExecution,
            translatedExecution,
            env.target()
        );
        return TpchQueryDiagnostics.format(diagnostics)
            + "source SQL:"
            + System.lineSeparator()
            + query.sql()
            + System.lineSeparator()
            + "translated SQL:"
            + System.lineSeparator()
            + translatedSql;
    }

    private static TpchEnvironmentFactory.ConnectionConfig resolveConnectionConfig() {
        var jdbcUrl = configuredOrDerivedJdbcUrl();
        var user = configuredOrEnv(JDBC_USER_PROPERTY, "POSTGRES_USER");
        var password = configuredOrEnv(JDBC_PASSWORD_PROPERTY, "POSTGRES_PASSWORD");

        if (jdbcUrl == null || jdbcUrl.isBlank() || user == null || user.isBlank()
            || password == null || password.isBlank()) {
            throw new IllegalStateException(
                "Postgres TPCH connection is not configured. Provide nnsql.tpch.jdbcUrl, "
                    + "nnsql.tpch.jdbcUser, nnsql.tpch.jdbcPassword or POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD."
            );
        }

        return new TpchEnvironmentFactory.ConnectionConfig(jdbcUrl, user, password);
    }

    private static String configuredOrDerivedJdbcUrl() {
        var configured = System.getProperty(JDBC_URL_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return configured.strip();
        }

        var database = System.getenv("POSTGRES_DB");
        if (database == null || database.isBlank()) {
            return null;
        }

        return "jdbc:postgresql://localhost:5432/" + database.strip();
    }

    private static String configuredOrEnv(String propertyName, String envName) {
        var configured = System.getProperty(propertyName);
        if (configured != null && !configured.isBlank()) {
            return configured.strip();
        }
        var environment = System.getenv(envName);
        return environment == null || environment.isBlank() ? null : environment.strip();
    }

    private static String configuredOrDefault(String propertyName, String defaultValue) {
        var configured = System.getProperty(propertyName);
        return configured == null || configured.isBlank() ? defaultValue : configured.strip();
    }
}
