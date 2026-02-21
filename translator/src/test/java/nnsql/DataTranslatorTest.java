package nnsql;

import nnsql.data.DataTranslator;
import nnsql.query.SchemaRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataTranslatorTest {

    @Test
    void emitsUnsigned128BitIdsForGeneratedRows(@TempDir Path tempDir) throws IOException {
        var sourceCsv = tempDir.resolve("events.csv");
        Files.writeString(sourceCsv, """
            a,b
            1,x
            2,y
            """);

        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable(
            "events",
            List.of("a", "b"),
            Map.of("a", "INTEGER", "b", "VARCHAR"),
            List.of()
        );

        var outputDir = tempDir.resolve("out");
        var translator = new DataTranslator(schemaRegistry);
        translator.translate("events", sourceCsv, outputDir);

        var idLines = Files.readAllLines(outputDir.resolve("events__ID.csv"));
        assertEquals("id", idLines.getFirst());
        assertEquals(3, idLines.size());
        assertTrue(idLines.get(1).matches("[0-9]{1,39}"));
        assertTrue(idLines.get(2).matches("[0-9]{1,39}"));
        assertNotEquals(idLines.get(1), idLines.get(2));

        var aLines = Files.readAllLines(outputDir.resolve("events_a.csv"));
        assertEquals("id,v", aLines.getFirst());
        assertTrue(aLines.get(1).startsWith(idLines.get(1) + ","));
        assertTrue(aLines.get(2).startsWith(idLines.get(2) + ","));
    }

    @Test
    void keepsPrimaryKeyValueAsRowId(@TempDir Path tempDir) throws IOException {
        var sourceCsv = tempDir.resolve("users.csv");
        Files.writeString(sourceCsv, """
            user_id,name
            10,alice
            20,bob
            """);

        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable(
            "users",
            List.of("user_id", "name"),
            Map.of("user_id", "INTEGER", "name", "VARCHAR"),
            List.of("user_id")
        );

        var outputDir = tempDir.resolve("out");
        var translator = new DataTranslator(schemaRegistry);
        translator.translate("users", sourceCsv, outputDir);

        var idLines = Files.readAllLines(outputDir.resolve("users__ID.csv"));
        assertEquals(List.of("id", "10", "20"), idLines);
    }
}
