package nnsql;

import nnsql.dml.DMLTranslator;
import nnsql.query.SchemaRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DMLTranslatorTest {

    @Test
    void usesUnsigned128BitHashesForGeneratedIds() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable(
            "R",
            List.of("A", "B"),
            Map.of("A", "INTEGER", "B", "VARCHAR"),
            List.of()
        );
        var translator = new DMLTranslator(schemaRegistry);

        var sql = translator.translate("INSERT INTO R (A, B) VALUES (1, 'x')");

        assertTrue(sql.contains("INSERT INTO R__ID (id) VALUES ("));
        var matcher = Pattern.compile("VALUES \\(([0-9]{1,39})(?:\\)|,)").matcher(sql);
        var hashes = new ArrayList<String>();
        while (matcher.find()) {
            hashes.add(matcher.group(1));
        }
        assertEquals(3, hashes.size());
        assertEquals(1, new HashSet<>(hashes).size());
        assertFalse(sql.contains("from_hex("));
    }

    @Test
    void keepsExplicitPrimaryKeyValuesAsIds() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable(
            "R",
            List.of("id", "A"),
            Map.of("id", "BIGINT", "A", "VARCHAR"),
            List.of("id")
        );
        var translator = new DMLTranslator(schemaRegistry);

        var sql = translator.translate("INSERT INTO R (id, A) VALUES (42, 'x')");

        assertTrue(sql.contains("INSERT INTO R__ID (id) VALUES (42)"));
        assertFalse(sql.contains("from_hex("));
    }
}
