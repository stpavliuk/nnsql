package nnsql.tpch.framework;

import nnsql.query.SchemaRegistry;

import java.util.ArrayList;
import java.util.List;

final class TpchTranslatedSchemaBuilder {

    private TpchTranslatedSchemaBuilder() {
    }

    static List<String> buildStatements(TpchFixtureSet fixtureSet, SchemaRegistry schemaRegistry) {
        var statements = new ArrayList<String>();

        for (var tableName : fixtureSet.tables()) {
            var schema = schemaRegistry.getSchema(tableName);
            if (schema == null) {
                throw new IllegalStateException("Missing schema registry entry for table " + tableName);
            }

            statements.add("""
                CREATE TABLE %s__ID (
                    id UUID PRIMARY KEY
                )""".formatted(tableName));

            for (var attribute : schema.attributes()) {
                var type = schema.columnTypes().get(attribute);
                if (type == null || type.isBlank()) {
                    throw new IllegalStateException(
                        "Missing column type for %s.%s".formatted(tableName, attribute)
                    );
                }

                statements.add("""
                    CREATE TABLE %s_%s (
                        id UUID PRIMARY KEY,
                        v %s,
                        FOREIGN KEY (id) REFERENCES %s__ID(id)
                    )""".formatted(tableName, attribute, type, tableName));
                statements.add("""
                    CREATE INDEX %s_%s_v_idx
                    ON %s_%s (v)
                    """.formatted(tableName, attribute, tableName, attribute));
            }
        }

        return statements;
    }
}
