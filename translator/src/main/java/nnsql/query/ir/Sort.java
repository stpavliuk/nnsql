package nnsql.query.ir;

import java.util.List;
import java.util.stream.Collectors;

public record Sort(IRNode input, List<SortKey> keys, Integer limit) implements IRNode {

    public Sort(IRNode input, List<SortKey> keys, Integer limit) {
        this.input = input;
        this.keys = List.copyOf(keys);
        this.limit = limit;
    }

    @Override
    public String toString() {
        var orderBy = keys.isEmpty()
            ? ""
            : " ORDER BY " + keys.stream()
                .map(SortKey::toString)
                .collect(Collectors.joining(", "));
        var limitClause = limit != null ? " LIMIT " + limit : "";
        return "SORT" + orderBy + limitClause;
    }

    public record SortKey(String attribute, boolean descending) {
        @Override
        public String toString() {
            return attribute + (descending ? " DESC" : " ASC");
        }
    }
}
