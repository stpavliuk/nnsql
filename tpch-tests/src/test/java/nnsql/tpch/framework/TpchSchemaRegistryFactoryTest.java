package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TpchSchemaRegistryFactoryTest {

    @Test
    void overlaysManifestPrimaryKeysOntoSchemaRegistry() throws Exception {
        var fixtureSet = TpchFixtureSet.load("1");

        var schemaRegistry = TpchSchemaRegistryFactory.create(fixtureSet);

        assertEquals(
            java.util.List.of("l_orderkey", "l_linenumber"),
            schemaRegistry.getSchema("lineitem").primaryKeyColumns()
        );
        assertEquals(
            java.util.List.of("ps_partkey", "ps_suppkey"),
            schemaRegistry.getSchema("partsupp").primaryKeyColumns()
        );
        assertEquals("DECIMAL (15, 2)", schemaRegistry.getSchema("orders").columnTypes().get("o_totalprice"));
        assertNotNull(schemaRegistry.getSchema("customer"));
    }
}
