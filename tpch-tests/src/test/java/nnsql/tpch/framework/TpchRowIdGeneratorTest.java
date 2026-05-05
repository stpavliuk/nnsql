package nnsql.tpch.framework;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TpchRowIdGeneratorTest {

    @Test
    void hashesSingleColumnPrimaryKeysIntoStableUuidIds() {
        var rowId = TpchRowIdGenerator.rowId(
            List.of("user_id", "name"),
            List.of("10", "alice"),
            List.of("user_id"),
            0
        );

        assertEquals("31ef5241-9c50-52f6-ac35-e989ab33e252", rowId);
    }

    @Test
    void hashesCompositePrimaryKeysInManifestOrder() {
        var rowId = TpchRowIdGenerator.rowId(
            List.of("l_orderkey", "l_linenumber", "payload"),
            List.of("1", "2", "x"),
            List.of("l_orderkey", "l_linenumber"),
            0
        );

        assertEquals("a33d7471-0ae9-40cd-5cd8-367f85045c56", rowId);
    }

    @Test
    void hashesFullRowPlusRowIndexWhenNoPrimaryKeyExists() {
        var rowId = TpchRowIdGenerator.rowId(
            List.of("a", "b"),
            List.of("1", "x"),
            List.of(),
            0
        );

        assertEquals("4298313d-a1cd-976a-8ece-1e9b620ddf22", rowId);
    }

    @Test
    void rejectsBlankPrimaryKeyValues() {
        assertThrows(IllegalArgumentException.class, () ->
            TpchRowIdGenerator.rowId(
                List.of("user_id", "name"),
                List.of("", "alice"),
                List.of("user_id"),
                0
            )
        );
    }
}
