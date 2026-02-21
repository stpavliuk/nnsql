package nnsql.tpch.framework;

import nnsql.data.DataTranslator;
import nnsql.ddl.DDLTranslator;
import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.*;

import java.nio.file.*;
import java.sql.*;

public class TranslatedDbExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final double SCALE_FACTOR = 0.01;
    private static final String DB_DIR_PROPERTY = "nnsql.tpch.dbDir";
    private static final String SOURCE_DB_FILE = "source.duckdb";
    private static final String TARGET_DB_FILE = "target.duckdb";
    private static final ExtensionContext.Namespace NS =
        ExtensionContext.Namespace.create(TranslatedDbExtension.class);

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        var dbDir = resolveDbDirectory();
        var sourceDbPath = dbDir.resolve(SOURCE_DB_FILE);
        var targetDbPath = dbDir.resolve(TARGET_DB_FILE);
        var provider = new TpchDataProvider(SCALE_FACTOR);
        var cached = Files.exists(sourceDbPath) && Files.exists(targetDbPath);

        var env = cached
            ? buildCachedEnvironment(sourceDbPath, targetDbPath, dbDir, provider)
            : buildGeneratedEnvironment(sourceDbPath, targetDbPath, dbDir, provider);
        ctx.getStore(NS).put("env", env);
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        var env = ctx.getStore(NS).remove("env", TranslatedDbEnvironment.class);
        if (env != null) env.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return pc.getParameter().getType() == TranslatedDbEnvironment.class;
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
        return ec.getStore(NS).get("env", TranslatedDbEnvironment.class);
    }

    private static TranslatedDbEnvironment buildCachedEnvironment(
        Path sourceDbPath,
        Path targetDbPath,
        Path dbDir,
        TpchDataProvider provider
    ) throws Exception {
        System.out.println("Reusing cached TPC-H databases in " + dbDir);
        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            sourceConn = DriverManager.getConnection(duckDbJdbcUrl(sourceDbPath));
            targetConn = DriverManager.getConnection(duckDbJdbcUrl(targetDbPath));

            loadTpchOrSkip(sourceConn);
            var schemaRegistry = reconstructSchemaRegistry(sourceConn);
            return buildEnvironment(sourceConn, targetConn, schemaRegistry, provider, dbDir);
        } catch (Exception e) {
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
            throw e;
        }
    }

    private static TranslatedDbEnvironment buildGeneratedEnvironment(
        Path sourceDbPath,
        Path targetDbPath,
        Path dbDir,
        TpchDataProvider provider
    ) throws Exception {
        System.out.println("Generating TPC-H databases in " + dbDir);

        deleteDatabaseArtifacts(sourceDbPath);
        deleteDatabaseArtifacts(targetDbPath);

        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            sourceConn = DriverManager.getConnection(duckDbJdbcUrl(sourceDbPath));
            generateTpchOrSkip(provider, sourceConn);

            var schemaRegistry = new SchemaRegistry();
            targetConn = DriverManager.getConnection(duckDbJdbcUrl(targetDbPath));
            initializeTranslatedTarget(sourceConn, targetConn, schemaRegistry, dbDir);

            return buildEnvironment(sourceConn, targetConn, schemaRegistry, provider, dbDir);
        } catch (Exception e) {
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
            throw e;
        }
    }

    private static void loadTpchOrSkip(Connection sourceConn) {
        try {
            try (var stmt = sourceConn.createStatement()) {
                stmt.execute("LOAD tpch");
            }
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "TPC-H extension not available: " + e.getMessage());
        }
    }

    private static void generateTpchOrSkip(TpchDataProvider provider, Connection sourceConn) {
        try {
            provider.generate(sourceConn);
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "TPC-H extension not available: " + e.getMessage());
        }
    }

    private static void initializeTranslatedTarget(
        Connection sourceConn,
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        Path dbDir
    ) throws Exception {
        createTranslatedSchema(sourceConn, targetConn, schemaRegistry);
        loadTranslatedData(sourceConn, targetConn, schemaRegistry, dbDir);
    }

    private static void createTranslatedSchema(
        Connection sourceConn,
        Connection targetConn,
        SchemaRegistry schemaRegistry
    ) throws Exception {
        var ddlStatements = DdlExtractor.extractCreateStatements(sourceConn);
        var ddlTranslator = new DDLTranslator(schemaRegistry);

        for (var ddl : ddlStatements) {
            var translated6nf = ddlTranslator.translate(ddl);
            for (var stmt : translated6nf.split(";")) {
                var trimmed = stmt.strip();
                if (!trimmed.isEmpty()) {
                    targetConn.createStatement().execute(trimmed);
                }
            }
        }
    }

    private static void loadTranslatedData(
        Connection sourceConn,
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        Path dbDir
    ) throws Exception {
        var dataTranslator = new DataTranslator(schemaRegistry);
        var tableNames = extractTableNames(sourceConn);

        for (var tableName : tableNames) {
            var csvPath = dbDir.resolve(tableName + ".csv");
            Files.deleteIfExists(csvPath);
            exportTableToCsv(sourceConn, tableName, csvPath);

            var tableOutputDir = dbDir.resolve(tableName + "_6nf");
            deleteDirectoryIfExists(tableOutputDir);
            dataTranslator.translate(tableName, csvPath, tableOutputDir);

            import6nfCsvs(targetConn, tableName, tableOutputDir);
            Files.deleteIfExists(csvPath);
            deleteDirectoryIfExists(tableOutputDir);
        }
    }

    private static TranslatedDbEnvironment buildEnvironment(
        Connection sourceConn,
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        TpchDataProvider provider,
        Path dbDir
    ) {
        var queries = provider.queries(sourceConn);
        var queryTranslator = new QueryTranslator(schemaRegistry, new SQLIRRenderer());
        return new TranslatedDbEnvironment(sourceConn, targetConn, queryTranslator, queries, dbDir, false);
    }

    private static java.util.List<String> extractTableNames(Connection conn) throws SQLException {
        var names = new java.util.ArrayList<String>();
        try (var rs = conn.createStatement().executeQuery(
            "SELECT DISTINCT table_name FROM information_schema.columns WHERE table_schema = 'main' ORDER BY table_name")) {
            while (rs.next()) names.add(rs.getString("table_name"));
        }
        return names;
    }

    private static void exportTableToCsv(Connection conn, String tableName, Path csvPath) throws SQLException {
        conn.createStatement().execute(
            "COPY %s TO '%s' (HEADER, DELIMITER ',')".formatted(tableName, csvPath));
    }

    private static void import6nfCsvs(Connection conn, String tableName, Path dir) throws Exception {
        var idCsv = dir.resolve(tableName + "__ID.csv");
        if (java.nio.file.Files.exists(idCsv)) {
            conn.createStatement().execute(
                "COPY %s__ID FROM '%s' (HEADER, DELIMITER ',')".formatted(tableName, idCsv));
        }

        try (var stream = java.nio.file.Files.list(dir)) {
            var attrFiles = stream
                .filter(p -> p.getFileName().toString().startsWith(tableName + "_"))
                .filter(p -> !p.getFileName().toString().equals(tableName + "__ID.csv"))
                .sorted()
                .toList();

            for (var csvFile : attrFiles) {
                var fileName = csvFile.getFileName().toString();
                var tblName = fileName.substring(0, fileName.length() - 4);
                try {
                    conn.createStatement().execute(
                        "COPY %s FROM '%s' (HEADER, DELIMITER ',')".formatted(tblName, csvFile));
                } catch (SQLException e) {
                    System.err.println("Warning: failed to load " + csvFile.getFileName() + ": " + e.getMessage());
                }
            }
        }
    }

    private static SchemaRegistry reconstructSchemaRegistry(Connection sourceConn) throws SQLException {
        var schemaRegistry = new SchemaRegistry();
        var ddlTranslator = new DDLTranslator(schemaRegistry);
        var ddlStatements = DdlExtractor.extractCreateStatements(sourceConn);
        for (var ddl : ddlStatements) {
            ddlTranslator.translate(ddl); // populates schemaRegistry as side effect
        }
        return schemaRegistry;
    }

    private static Path resolveDbDirectory() throws Exception {
        var configured = System.getProperty(DB_DIR_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            var dbDir = Paths.get(configured).toAbsolutePath().normalize();
            Files.createDirectories(dbDir);
            return dbDir;
        }
        var dbDir = Paths.get("build", "tpch-db").toAbsolutePath().normalize();
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

    private static void deleteDirectoryIfExists(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
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
