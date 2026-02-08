package nnsql;

import nnsql.ddl.DDLTranslator;
import nnsql.dml.DMLTranslator;
import nnsql.duckdb.Database;
import nnsql.query.SchemaRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.function.Predicate;

public class ImportData {

    public static final String DATA_FILE = "duckdb/data.sql";
    public static final String SCHEMA_FILE = "duckdb/schema.sql";

    public static final String DB_FILE = "duckdb/nnsql.duckdb";

    public Connection connection;

    public ImportData(Connection connection) {
        this.connection = connection;
    }

    static void main() {
        var schemaRegistry = new SchemaRegistry();
        var ddlTranslator = new DDLTranslator(schemaRegistry);
        var dmlTranslator = new DMLTranslator(schemaRegistry);

        try (var conn = Database.getConnection(Path.of(DB_FILE))) {
            var importer = new ImportData(conn);
            importer.translateExecute(schemaRegistry, SCHEMA_FILE, ddlTranslator);
            importer.translateExecute(schemaRegistry, DATA_FILE, dmlTranslator);
        } catch (Exception e) {
            System.err.println("Error during import: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    void translateExecute(SchemaRegistry schemaRegistry, String filename, Translator translator) {
        try (var schemaLines = Files.lines(Path.of(filename))) {
            schemaLines
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .filter(it -> !it.startsWith("--"))
                .map(this::quoteDateLiterals)
                .map(translator::translate)
                .forEach(this::executeSQL);
        } catch (RuntimeException e) {
            System.err.println("Error translating/executing schema: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error reading schema file: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private String quoteDateLiterals(String sql) {
        // exported data contained unquoted date literals like 1996-03-13
        // kinda fix for that :)
        return sql.replaceAll(
            "(?<!['])\\b(\\d{4}-\\d{2}-\\d{2})\\b(?!')",
            "'$1'"
        );
    }

    void executeSQL(String sql) {
        try (var stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Error executing SQL: " + sql, e);
        }
    }

}
