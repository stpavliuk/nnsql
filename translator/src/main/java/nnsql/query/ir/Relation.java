package nnsql.query.ir;

import java.util.List;
import java.util.Optional;

public sealed interface Relation {
    String alias();
    List<String> attributes();

    static Table table(String tableName, String alias, List<String> attributes) {
        return new Table(tableName, alias, attributes);
    }

    static Table table(String tableName, List<String> attributes) {
        return new Table(tableName, tableName, attributes);
    }

    static Subquery subquery(String alias, IRNode ir, List<String> attributes) {
        return new Subquery(alias, ir, attributes);
    }

    record Table(String tableName, String alias, List<String> attributes)
        implements Relation {}

    record Subquery(String alias, IRNode ir, List<String> attributes)
        implements Relation {}
}
