package nnsql.tpch.framework;

import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.postgresql.PGConnection;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

final class TpchEnvironmentFactory {

    private static final CSVFormat INPUT_CSV = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .get();
    private static final CSVFormat OUTPUT_CSV = CSVFormat.DEFAULT.builder()
        .setRecordSeparator(System.lineSeparator())
        .get();

    private TpchEnvironmentFactory() {
    }

    static TranslatedDbEnvironment openOrBuildEnvironment(
        ConnectionConfig config,
        String sourceSchema,
        String targetSchema,
        TpchFixtureSet fixtureSet,
        double nullRate,
        TpchDataProvider provider
    ) throws Exception {
        if (schemasExist(config, sourceSchema, targetSchema)) {
            try {
                return buildCachedEnvironment(config, sourceSchema, targetSchema, fixtureSet, provider);
            } catch (Exception e) {
                System.err.println("Rebuilding cached TPC-H Postgres schemas after open failure: " + e.getMessage());
                dropSchema(config, sourceSchema);
                dropSchema(config, targetSchema);
            }
        }

        return buildGeneratedEnvironment(config, sourceSchema, targetSchema, fixtureSet, nullRate, provider);
    }

    static String cacheKeyFor(String scaleFactor, double nullRate) throws IOException {
        return TpchFixtureSet.cacheKey(nullRate, TpchFixtureSet.load(scaleFactor));
    }

    static void refreshOptimizerStatistics(Connection conn) throws SQLException {
        executeSql(conn, "ANALYZE");
    }

    private static TranslatedDbEnvironment buildCachedEnvironment(
        ConnectionConfig config,
        String sourceSchema,
        String targetSchema,
        TpchFixtureSet fixtureSet,
        TpchDataProvider provider
    ) throws Exception {
        System.out.println("Reusing cached TPC-H Postgres schemas " + sourceSchema + " and " + targetSchema);
        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            sourceConn = openConnection(config, sourceSchema);
            targetConn = openConnection(config, targetSchema);
            var schemaRegistry = TpchSchemaRegistryFactory.create(fixtureSet);
            refreshOptimizerStatistics(sourceConn);
            refreshOptimizerStatistics(targetConn);
            return buildEnvironment(sourceConn, targetConn, schemaRegistry, provider);
        } catch (Exception e) {
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
            throw e;
        }
    }

    private static TranslatedDbEnvironment buildGeneratedEnvironment(
        ConnectionConfig config,
        String sourceSchema,
        String targetSchema,
        TpchFixtureSet fixtureSet,
        double nullRate,
        TpchDataProvider provider
    ) throws Exception {
        System.out.println("Building TPC-H Postgres schemas " + sourceSchema + " and " + targetSchema);
        dropSchema(config, sourceSchema);
        dropSchema(config, targetSchema);
        createSchema(config, sourceSchema);
        createSchema(config, targetSchema);

        var workDir = Path.of("build", "tpch-work", sourceSchema + "__" + targetSchema).toAbsolutePath().normalize();
        deleteDirectoryContents(workDir);
        Files.createDirectories(workDir);

        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            var stagedCsvDir = TpchNullPreprocessor.stageFixtures(fixtureSet, nullRate, workDir.resolve("staged"));
            sourceConn = openConnection(config, sourceSchema);
            initializeSourceDatabase(sourceConn, fixtureSet, stagedCsvDir);

            var schemaRegistry = TpchSchemaRegistryFactory.create(fixtureSet);
            targetConn = openConnection(config, targetSchema);
            initializeTranslatedTarget(
                targetConn,
                schemaRegistry,
                fixtureSet,
                stagedCsvDir,
                workDir.resolve("translated")
            );

            refreshOptimizerStatistics(sourceConn);
            refreshOptimizerStatistics(targetConn);
            return buildEnvironment(sourceConn, targetConn, schemaRegistry, provider);
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
            copyCsv(sourceConn, tableName, stagedCsvDir.resolve(tableName + ".csv"));
        }
        createSourceIndexes(sourceConn, TpchSchemaRegistryFactory.create(fixtureSet));
    }

    private static void createSourceIndexes(
        Connection sourceConn,
        SchemaRegistry schemaRegistry
    ) throws SQLException {
        for (var tableName : schemaRegistry.tableNames()) {
            var schema = schemaRegistry.getSchema(tableName);
            for (var attribute : schema.attributes()) {
                executeSql(sourceConn, """
                    CREATE INDEX %s_%s_idx
                    ON %s (%s)
                    """.formatted(tableName, attribute, tableName, attribute));
            }
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
        for (var statement : TpchTranslatedSchemaBuilder.buildStatements(fixtureSet, schemaRegistry)) {
            executeSql(targetConn, statement);
        }
    }

    private static void loadTranslatedData(
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        TpchFixtureSet fixtureSet,
        Path stagedCsvDir,
        Path translatedDir
    ) throws Exception {
        deleteDirectoryContents(translatedDir);
        Files.createDirectories(translatedDir);

        for (var tableName : fixtureSet.tables()) {
            var csvPath = stagedCsvDir.resolve(tableName + ".csv");
            var tableOutputDir = translatedDir.resolve(tableName + "_6nf");
            deleteDirectoryContents(tableOutputDir);
            Files.createDirectories(tableOutputDir);
            materializeTranslatedCsvs(
                csvPath,
                tableOutputDir,
                schemaRegistry.getSchema(tableName).attributes(),
                fixtureSet.primaryKeys(tableName),
                tableName
            );
            importTranslatedCsvs(targetConn, tableName, tableOutputDir, schemaRegistry.getSchema(tableName).attributes());
        }
    }

    private static TranslatedDbEnvironment buildEnvironment(
        Connection sourceConn,
        Connection targetConn,
        SchemaRegistry schemaRegistry,
        TpchDataProvider provider
    ) {
        var queries = provider.queries();
        var queryTranslator = new QueryTranslator(schemaRegistry, SQLIRRenderer.postgresCompatible());
        return new TranslatedDbEnvironment(sourceConn, targetConn, queryTranslator, queries, null, false);
    }

    private static boolean schemasExist(ConnectionConfig config, String sourceSchema, String targetSchema) throws SQLException {
        try (var connection = openConnection(config, null)) {
            return schemaExists(connection, sourceSchema) && schemaExists(connection, targetSchema);
        }
    }

    private static boolean schemaExists(Connection connection, String schemaName) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.schemata
                WHERE schema_name = ?
                """)) {
            stmt.setString(1, schemaName);
            try (var rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void createSchema(ConnectionConfig config, String schemaName) throws SQLException {
        try (var connection = openConnection(config, null)) {
            executeSql(connection, "CREATE SCHEMA " + schemaName);
        }
    }

    private static void dropSchema(ConnectionConfig config, String schemaName) throws SQLException {
        try (var connection = openConnection(config, null)) {
            executeSql(connection, "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
        }
    }

    private static Connection openConnection(ConnectionConfig config, String schemaName) throws SQLException {
        var props = new Properties();
        props.setProperty("user", config.user());
        props.setProperty("password", config.password());
        var connection = DriverManager.getConnection(config.jdbcUrl(), props);

        try {
            executeSql(connection, "SET jit = off");
        } catch (SQLException e) {
            closeQuietly(connection);
            throw e;
        }

        if (schemaName != null && !schemaName.isBlank()) {
            try {
                executeSql(connection, "SET search_path TO " + schemaName + ", public");
            } catch (SQLException e) {
                closeQuietly(connection);
                throw e;
            }
        }

        return connection;
    }

    private static void materializeTranslatedCsvs(
        Path sourceCsv,
        Path outputDir,
        List<String> attributes,
        List<String> primaryKeyColumns,
        String tableName
    ) throws IOException {
        try (var reader = Files.newBufferedReader(sourceCsv, StandardCharsets.UTF_8);
             var parser = INPUT_CSV.parse(reader);
             var writers = TranslatedCsvWriters.open(outputDir, tableName, attributes)) {
            var headers = List.copyOf(parser.getHeaderNames());
            int rowIndex = 0;
            for (var record : parser) {
                var values = new ArrayList<String>(headers.size());
                for (int i = 0; i < headers.size(); i++) {
                    values.add(record.get(i));
                }

                var rowId = TpchRowIdGenerator.rowId(headers, values, primaryKeyColumns, rowIndex);
                writers.writeId(rowId);
                for (int i = 0; i < headers.size(); i++) {
                    var value = values.get(i);
                    if (isNullOrEmpty(value)) {
                        continue;
                    }
                    writers.writeAttribute(headers.get(i), rowId, value);
                }
                rowIndex++;
            }
        }
    }

    private static void importTranslatedCsvs(
        Connection conn,
        String tableName,
        Path dir,
        List<String> attributes
    ) throws Exception {
        copyCsv(conn, tableName + "__ID", dir.resolve(tableName + "__ID.csv"));
        for (var attribute : attributes) {
            copyCsv(conn, tableName + "_" + attribute, dir.resolve(tableName + "_" + attribute + ".csv"));
        }
    }

    private static void copyCsv(Connection conn, String tableName, Path csvPath) throws Exception {
        try (var reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            var copyManager = conn.unwrap(PGConnection.class).getCopyAPI();
            copyManager.copyIn(
                "COPY " + tableName + " FROM STDIN WITH (FORMAT csv, HEADER true)",
                reader
            );
        }
    }

    private static void executeSqlScript(Connection conn, String sqlScript) throws SQLException {
        for (var statement : sqlScript.split(";")) {
            var trimmed = statement.strip();
            if (!trimmed.isEmpty()) {
                executeSql(conn, trimmed);
            }
        }
    }

    private static void executeSql(Connection conn, String sql) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static String readUtf8(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static boolean isNullOrEmpty(String value) {
        if (value == null) {
            return true;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.equals("\\N");
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

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception _) {
        }
    }

    record ConnectionConfig(String jdbcUrl, String user, String password) {
    }

    private static final class TranslatedCsvWriters implements AutoCloseable {
        private final CSVPrinter idWriter;
        private final LinkedHashMap<String, CSVPrinter> attributeWriters;

        private TranslatedCsvWriters(CSVPrinter idWriter, LinkedHashMap<String, CSVPrinter> attributeWriters) {
            this.idWriter = idWriter;
            this.attributeWriters = attributeWriters;
        }

        static TranslatedCsvWriters open(Path outputDir, String tableName, List<String> attributes) throws IOException {
            var opened = new ArrayList<Closeable>();
            try {
                var idWriter = OUTPUT_CSV.print(
                    Files.newBufferedWriter(outputDir.resolve(tableName + "__ID.csv"), StandardCharsets.UTF_8)
                );
                opened.add(idWriter);
                idWriter.printRecord("id");

                var attributeWriters = new LinkedHashMap<String, CSVPrinter>();
                for (var attribute : attributes) {
                    var writer = OUTPUT_CSV.print(
                        Files.newBufferedWriter(outputDir.resolve(tableName + "_" + attribute + ".csv"), StandardCharsets.UTF_8)
                    );
                    opened.add(writer);
                    writer.printRecord("id", "v");
                    attributeWriters.put(attribute, writer);
                }

                return new TranslatedCsvWriters(idWriter, attributeWriters);
            } catch (IOException e) {
                closeAll(opened);
                throw e;
            }
        }

        void writeId(String rowId) throws IOException {
            idWriter.printRecord(rowId);
        }

        void writeAttribute(String attribute, String rowId, String value) throws IOException {
            var writer = attributeWriters.get(attribute);
            if (writer == null) {
                throw new IllegalArgumentException("No writer configured for attribute " + attribute);
            }
            writer.printRecord(rowId, value);
        }

        @Override
        public void close() throws IOException {
            var closeables = new ArrayList<Closeable>(attributeWriters.size() + 1);
            closeables.add(idWriter);
            closeables.addAll(attributeWriters.values());
            closeAll(closeables);
        }
    }

    private static void closeAll(List<? extends Closeable> closeables) throws IOException {
        IOException failure = null;
        for (var closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
