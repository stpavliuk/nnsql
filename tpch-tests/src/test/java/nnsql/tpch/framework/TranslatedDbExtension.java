package nnsql.tpch.framework;

import nnsql.data.DataTranslator;
import nnsql.ddl.DDLTranslator;
import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Comparator;

public class TranslatedDbExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    static final String DEFAULT_SCALE_FACTOR = "0.5";
    static final double DEFAULT_NULL_RATE = 0.7d;

    private static final String DB_DIR_PROPERTY = "nnsql.tpch.dbDir";
    private static final String NULL_RATE_PROPERTY = "nnsql.tpch.nullRate";
    private static final String SCALE_FACTOR_PROPERTY = "nnsql.tpch.scaleFactor";
    private static final String SOURCE_DB_FILE = "source.duckdb";
    private static final String TARGET_DB_FILE = "target.duckdb";
    private static final ExtensionContext.Namespace NS =
        ExtensionContext.Namespace.create(TranslatedDbExtension.class);

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        var provider = new TpchDataProvider();
        var scaleFactor = resolveScaleFactor();
        var nullRate = resolveNullRate();
        var fixtureSet = TpchFixtureSet.load(scaleFactor);
        var cacheDir = resolveDbDirectory().resolve(TpchFixtureSet.cacheKey(nullRate, fixtureSet));
        Files.createDirectories(cacheDir);

        var sourceDbPath = cacheDir.resolve(SOURCE_DB_FILE);
        var targetDbPath = cacheDir.resolve(TARGET_DB_FILE);
        var env = openOrBuildEnvironment(sourceDbPath, targetDbPath, cacheDir, fixtureSet, nullRate, provider);
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

    private static TranslatedDbEnvironment openOrBuildEnvironment(
        Path sourceDbPath,
        Path targetDbPath,
        Path cacheDir,
        TpchFixtureSet fixtureSet,
        double nullRate,
        TpchDataProvider provider
    ) throws Exception {
        if (Files.exists(sourceDbPath) && Files.exists(targetDbPath)) {
            try {
                return buildCachedEnvironment(sourceDbPath, targetDbPath, cacheDir, fixtureSet, provider);
            } catch (Exception e) {
                System.err.println("Rebuilding cached TPC-H databases after open failure: " + e.getMessage());
                deleteDirectoryContents(cacheDir);
            }
        }
        return buildGeneratedEnvironment(sourceDbPath, targetDbPath, cacheDir, fixtureSet, nullRate, provider);
    }

    private static TranslatedDbEnvironment buildCachedEnvironment(
        Path sourceDbPath,
        Path targetDbPath,
        Path cacheDir,
        TpchFixtureSet fixtureSet,
        TpchDataProvider provider
    ) throws Exception {
        System.out.println("Reusing cached TPC-H databases in " + cacheDir);
        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            sourceConn = DriverManager.getConnection(duckDbJdbcUrl(sourceDbPath));
            targetConn = DriverManager.getConnection(duckDbJdbcUrl(targetDbPath));
            var schemaRegistry = buildSchemaRegistry(fixtureSet);
            return buildEnvironment(sourceConn, targetConn, schemaRegistry, provider, cacheDir);
        } catch (Exception e) {
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
            throw e;
        }
    }

    private static TranslatedDbEnvironment buildGeneratedEnvironment(
        Path sourceDbPath,
        Path targetDbPath,
        Path cacheDir,
        TpchFixtureSet fixtureSet,
        double nullRate,
        TpchDataProvider provider
    ) throws Exception {
        System.out.println("Building TPC-H databases in " + cacheDir);
        Files.createDirectories(cacheDir);
        deleteDatabaseArtifacts(sourceDbPath);
        deleteDatabaseArtifacts(targetDbPath);

        var workDir = cacheDir.resolve("work");
        deleteDirectoryContents(workDir);
        Files.createDirectories(workDir);

        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            var stagedCsvDir = TpchNullPreprocessor.stageFixtures(fixtureSet, nullRate, workDir.resolve("staged"));
            sourceConn = DriverManager.getConnection(duckDbJdbcUrl(sourceDbPath));
            initializeSourceDatabase(sourceConn, fixtureSet, stagedCsvDir);

            var schemaRegistry = buildSchemaRegistry(fixtureSet);
            targetConn = DriverManager.getConnection(duckDbJdbcUrl(targetDbPath));
            initializeTranslatedTarget(targetConn, schemaRegistry, fixtureSet, stagedCsvDir, workDir.resolve("translated"));

            return buildEnvironment(sourceConn, targetConn, schemaRegistry, provider, cacheDir);
        } catch (Exception e) {
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
            throw e;
        } finally {
            deleteDirectoryContents(workDir);
        }
    }

    private static void initializeSourceDatabase(
        Connection sourceConn,
        TpchFixtureSet fixtureSet,
        Path stagedCsvDir
    ) throws Exception {
        executeSqlScript(sourceConn, readUtf8(fixtureSet.schemaPath()));
        for (var tableName : fixtureSet.tables()) {
            importCsv(sourceConn, tableName, stagedCsvDir.resolve(tableName + ".csv"));
        }
    }

    private static void initializeTranslatedTarget(
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        TpchFixtureSet fixtureSet,
        Path stagedCsvDir,
        Path translatedDir
    ) throws Exception {
        createTranslatedSchema(targetConn, schemaRegistry, fixtureSet);
        loadTranslatedData(targetConn, schemaRegistry, fixtureSet, stagedCsvDir, translatedDir);
    }

    private static void createTranslatedSchema(
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        TpchFixtureSet fixtureSet
    ) throws Exception {
        var ddlTranslator = new DDLTranslator(schemaRegistry);
        var translated6nf = ddlTranslator.translate(readUtf8(fixtureSet.schemaPath()));
        executeSqlScript(targetConn, translated6nf);
    }

    private static void loadTranslatedData(
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        TpchFixtureSet fixtureSet,
        Path stagedCsvDir,
        Path translatedDir
    ) throws Exception {
        var dataTranslator = new DataTranslator(schemaRegistry);
        deleteDirectoryContents(translatedDir);
        Files.createDirectories(translatedDir);

        for (var tableName : fixtureSet.tables()) {
            var csvPath = stagedCsvDir.resolve(tableName + ".csv");
            var tableOutputDir = translatedDir.resolve(tableName + "_6nf");
            deleteDirectoryContents(tableOutputDir);
            Files.createDirectories(tableOutputDir);
            dataTranslator.translate(tableName, csvPath, tableOutputDir);
            import6nfCsvs(targetConn, tableName, tableOutputDir);
        }
    }

    private static TranslatedDbEnvironment buildEnvironment(
        Connection sourceConn,
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        TpchDataProvider provider,
        Path cacheDir
    ) {
        var queries = provider.queries();
        var queryTranslator = new QueryTranslator(schemaRegistry, new SQLIRRenderer());
        return new TranslatedDbEnvironment(sourceConn, targetConn, queryTranslator, queries, cacheDir, false);
    }

    private static SchemaRegistry buildSchemaRegistry(TpchFixtureSet fixtureSet) throws IOException {
        var schemaRegistry = new SchemaRegistry();
        new DDLTranslator(schemaRegistry).translate(readUtf8(fixtureSet.schemaPath()));
        return schemaRegistry;
    }

    static String cacheKeyFor(String scaleFactor, double nullRate) throws IOException {
        return TpchFixtureSet.cacheKey(nullRate, TpchFixtureSet.load(scaleFactor));
    }

    private static void importCsv(Connection conn, String tableName, Path csvPath) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("COPY %s FROM '%s' (HEADER, DELIMITER ',')"
                .formatted(tableName, csvPath.toAbsolutePath()));
        }
    }

    private static void import6nfCsvs(Connection conn, String tableName, Path dir) throws Exception {
        var idCsv = dir.resolve(tableName + "__ID.csv");
        if (Files.exists(idCsv)) {
            importCsv(conn, tableName + "__ID", idCsv);
        }

        try (var stream = Files.list(dir)) {
            var attrFiles = stream
                .filter(path -> path.getFileName().toString().startsWith(tableName + "_"))
                .filter(path -> !path.getFileName().toString().equals(tableName + "__ID.csv"))
                .sorted()
                .toList();

            for (var csvFile : attrFiles) {
                var fileName = csvFile.getFileName().toString();
                var table = fileName.substring(0, fileName.length() - 4);
                try {
                    importCsv(conn, table, csvFile);
                } catch (SQLException e) {
                    System.err.println("Warning: failed to load " + csvFile.getFileName() + ": " + e.getMessage());
                }
            }
        }
    }

    private static void executeSqlScript(Connection conn, String sqlScript) throws SQLException {
        for (var statement : sqlScript.split(";")) {
            var trimmed = statement.strip();
            if (!trimmed.isEmpty()) {
                try (var stmt = conn.createStatement()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static String readUtf8(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
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

    private static Path resolveDbDirectory() throws Exception {
        var configured = System.getProperty(DB_DIR_PROPERTY);
        var dbDir = configured == null || configured.isBlank()
            ? Paths.get("build", "tpch-db").toAbsolutePath().normalize()
            : Paths.get(configured).toAbsolutePath().normalize();
        Files.createDirectories(dbDir);
        return dbDir;
    }

    private static String duckDbJdbcUrl(Path dbPath) {
        return "jdbc:duckdb:" + dbPath.toAbsolutePath();
    }

    private static void deleteDatabaseArtifacts(Path dbPath) throws Exception {
        Files.deleteIfExists(dbPath);
        var fileName = dbPath.getFileName().toString();
        Files.deleteIfExists(dbPath.resolveSibling(fileName + ".wal"));
    }

    private static void deleteDirectoryContents(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception _) {
                }
            });
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Exception _) {
        }
    }
}
