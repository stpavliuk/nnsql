package nnsql.query.renderer;

public record CTE(String name, String definition) {

    public String format() {
        return "%s AS (\n%s\n)".formatted(name, definition);
    }
}
