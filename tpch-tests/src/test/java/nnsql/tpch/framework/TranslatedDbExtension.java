package nnsql.tpch.framework;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TranslatedDbExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    static final String DEFAULT_SCALE_FACTOR = "1";
    static final double DEFAULT_NULL_RATE = 0.7d;

    private static final String NULL_RATE_PROPERTY = "nnsql.tpch.nullRate";
    private static final String SCALE_FACTOR_PROPERTY = "nnsql.tpch.scaleFactor";
    private static final String JDBC_URL_PROPERTY = "nnsql.tpch.jdbcUrl";
    private static final String JDBC_USER_PROPERTY = "nnsql.tpch.jdbcUser";
    private static final String JDBC_PASSWORD_PROPERTY = "nnsql.tpch.jdbcPassword";
    private static final String SOURCE_SCHEMA_PREFIX = "tpch_src_";
    private static final String TARGET_SCHEMA_PREFIX = "tpch_tgt_";
    private static final ExtensionContext.Namespace NS =
        ExtensionContext.Namespace.create(TranslatedDbExtension.class);

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        var provider = new TpchDataProvider();
        var scaleFactor = resolveScaleFactor();
        var nullRate = resolveNullRate();
        var fixtureSet = TpchFixtureSet.load(scaleFactor);
        var cacheKey = TpchFixtureSet.cacheKey(nullRate, fixtureSet);
        var sourceSchema = SOURCE_SCHEMA_PREFIX + cacheKey;
        var targetSchema = TARGET_SCHEMA_PREFIX + cacheKey;
        var config = resolveConnectionConfig();

        var env = TpchEnvironmentFactory.openOrBuildEnvironment(
            config,
            sourceSchema,
            targetSchema,
            fixtureSet,
            nullRate,
            provider
        );
        ctx.getStore(NS).put("env", env);
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        var env = ctx.getStore(NS).remove("env", TranslatedDbEnvironment.class);
        if (env != null) {
            env.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return pc.getParameter().getType() == TranslatedDbEnvironment.class;
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
        return ec.getStore(NS).get("env", TranslatedDbEnvironment.class);
    }

    private static String resolveScaleFactor() {
        var configured = System.getProperty(SCALE_FACTOR_PROPERTY);
        return configured == null || configured.isBlank() ? DEFAULT_SCALE_FACTOR : configured.strip();
    }

    private static double resolveNullRate() {
        var configured = System.getProperty(NULL_RATE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return DEFAULT_NULL_RATE;
        }
        try {
            var parsed = Double.parseDouble(configured);
            Assumptions.assumeTrue(parsed >= 0.0d && parsed <= 1.0d,
                "TPC-H null rate must be between 0 and 1");
            return parsed;
        } catch (NumberFormatException e) {
            Assumptions.assumeTrue(false, "Invalid TPC-H null rate: " + configured);
            return DEFAULT_NULL_RATE;
        }
    }

    private static TpchEnvironmentFactory.ConnectionConfig resolveConnectionConfig() {
        var jdbcUrl = configuredOrDerivedJdbcUrl();
        var user = configuredOrEnv(JDBC_USER_PROPERTY, "POSTGRES_USER");
        var password = configuredOrEnv(JDBC_PASSWORD_PROPERTY, "POSTGRES_PASSWORD");

        if (jdbcUrl == null || jdbcUrl.isBlank() || user == null || user.isBlank()
            || password == null || password.isBlank()) {
            Assumptions.assumeTrue(false,
                "Postgres TPCH connection is not configured. Provide nnsql.tpch.jdbcUrl, "
                    + "nnsql.tpch.jdbcUser, nnsql.tpch.jdbcPassword or POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD.");
            return new TpchEnvironmentFactory.ConnectionConfig("", "", "");
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
}
