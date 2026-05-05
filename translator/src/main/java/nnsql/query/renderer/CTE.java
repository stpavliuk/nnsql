package nnsql.query.renderer;

import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.Set;

public record CTE(String name, PlainSelect definition, Set<String> dependencies) {

    public String format() {
        return "%s AS (\n%s\n)".formatted(name, definition);
    }
}
