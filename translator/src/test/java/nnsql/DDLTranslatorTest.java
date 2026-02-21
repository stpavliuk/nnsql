package nnsql;

import nnsql.ddl.DDLTranslator;
import nnsql.query.SchemaRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DDLTranslatorTest {

    @Test
    void usesUnsignedHugeIntForGeneratedIds() {
        var translator = new DDLTranslator(new SchemaRegistry());

        var sql = translator.translate("CREATE TABLE events (a INTEGER, b VARCHAR)");

        assertTrue(sql.contains("CREATE TABLE events__ID"));
        assertTrue(sql.contains("id UHUGEINT PRIMARY KEY"));
        assertTrue(sql.contains("CREATE TABLE events_a"));
        assertTrue(sql.contains("CREATE TABLE events_b"));
    }

    @Test
    void preservesPrimaryKeyColumnTypeWhenAvailable() {
        var translator = new DDLTranslator(new SchemaRegistry());

        var sql = translator.translate("CREATE TABLE users (user_id BIGINT PRIMARY KEY, name VARCHAR)");

        assertTrue(sql.contains("id BIGINT PRIMARY KEY"));
        assertTrue(sql.contains("CREATE TABLE users_name"));
    }
}
