package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TpchFixtureSetTest {

    @Test
    void loadsFixtureDirectoryForScaleFactor() throws Exception {
        var fixtureSet = TpchFixtureSet.load("0.5");

        assertEquals("0.5", fixtureSet.scaleFactor());
        assertTrue(fixtureSet.schemaPath().endsWith("schema.sql"));
        assertEquals(8, fixtureSet.tables().size());
        assertTrue(fixtureSet.tables().contains("lineitem"));
        assertEquals(List.of("s_suppkey"), fixtureSet.primaryKeys("supplier"));
        assertEquals(List.of("ps_partkey", "ps_suppkey"), fixtureSet.primaryKeys("partsupp"));
        assertFalse(fixtureSet.fingerprint().isBlank());
    }

    @Test
    void cacheKeyChangesWithNullRate() throws Exception {
        var fixtureSet = TpchFixtureSet.load("0.5");

        var zeroRate = TpchFixtureSet.cacheKey(0.0d, fixtureSet);
        var defaultRate = TpchFixtureSet.cacheKey(0.7d, fixtureSet);

        assertNotEquals(zeroRate, defaultRate);
    }
}
