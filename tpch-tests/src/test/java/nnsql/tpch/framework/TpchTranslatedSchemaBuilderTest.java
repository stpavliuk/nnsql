package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TpchTranslatedSchemaBuilderTest {

    @Test
    void usesUuidIdsAndPreservesOriginalValueTypes() throws Exception {
        var fixtureSet = TpchFixtureSet.load("1");
        var schemaRegistry = TpchSchemaRegistryFactory.create(fixtureSet);

        var statements = TpchTranslatedSchemaBuilder.buildStatements(fixtureSet, schemaRegistry);
        var ddl = String.join("\n\n", statements);

        assertTrue(ddl.contains("CREATE TABLE customer__ID"));
        assertTrue(ddl.contains("id UUID PRIMARY KEY"));
        assertTrue(ddl.contains("CREATE TABLE customer_c_acctbal"));
        assertTrue(ddl.contains("v DECIMAL (15, 2)"));
        assertTrue(ddl.contains("CREATE TABLE lineitem_l_shipdate"));
        assertTrue(ddl.contains("v DATE"));
        assertTrue(ddl.contains("CREATE INDEX lineitem_l_shipdate_v_idx"));
        assertTrue(ddl.contains("ON lineitem_l_shipdate (v)"));
        assertTrue(ddl.contains("FOREIGN KEY (id) REFERENCES customer__ID(id)"));
    }
}
