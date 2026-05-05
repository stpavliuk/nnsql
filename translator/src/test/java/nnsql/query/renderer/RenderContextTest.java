package nnsql.query.renderer;

import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderContextTest {

    @Test
    void prunesUnusedCtesUsingStructuredDependencies() {
        var ctx = new RenderContext();
        ctx.addCTE("dep", selectFrom("base_table"));
        ctx.addCTE("root", selectFrom("dep"));
        ctx.addCTE("unused", selectFrom("dep"));

        var usedNames = ctx.getUsedCTEs(Set.of("root")).stream()
            .map(CTE::name)
            .toList();

        assertEquals(List.of("dep", "root"), usedNames);
    }

    @Test
    void dependencyDiscoveryDoesNotMatchStringLiterals() {
        var ctx = new RenderContext();
        ctx.addCTE("literal_only", selectStringLiteral("dep"));
        ctx.addCTE("dep", selectFrom("base_table"));

        var usedNames = ctx.getUsedCTEs(Set.of("literal_only")).stream()
            .map(CTE::name)
            .toList();

        assertEquals(List.of("literal_only"), usedNames);
    }

    private static PlainSelect selectFrom(String tableName) {
        var ps = new PlainSelect();
        ps.addSelectItem(new net.sf.jsqlparser.statement.select.AllColumns());
        ps.setFromItem(new net.sf.jsqlparser.schema.Table(tableName));
        return ps;
    }

    private static PlainSelect selectStringLiteral(String value) {
        var ps = new PlainSelect();
        ps.addSelectItem(new StringValue(value));
        return ps;
    }
}
