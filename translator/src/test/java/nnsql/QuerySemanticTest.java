package nnsql;

import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuerySemanticTest {
    @Test
    void globalCountOverEmptyInputReturnsZero() throws Exception {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("R", List.of("A", "B"));
        var translator = new QueryTranslator(schemaRegistry, new SQLIRRenderer());

        try (var connection = DriverManager.getConnection("jdbc:duckdb:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE R__ID(id UHUGEINT)");
            statement.execute("CREATE TABLE R_A(id UHUGEINT, v DOUBLE)");
            statement.execute("CREATE TABLE R_B(id UHUGEINT, v DOUBLE)");

            try (var result = statement.executeQuery(translator.translate("SELECT COUNT(*) AS cnt FROM R"))) {
                assertTrue(result.next());
                assertEquals(0L, result.getLong("cnt"));
                assertFalse(result.wasNull(), "COUNT over an empty global group should be 0, not SQL NULL");
                assertFalse(result.next());
            }
        }
    }
}
