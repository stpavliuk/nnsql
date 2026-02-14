package nnsql.query.builder;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Relations {

    static List<Relation> from(PlainSelect select, SchemaRegistry schema, AtomicInteger nodeIds) {
        var relations = new ArrayList<Relation>();
        relations.add(toRelation(select.getFromItem(), schema, nodeIds));

        if (select.getJoins() != null) {
            for (var join : select.getJoins()) {
                relations.add(toRelation(join.getFromItem(), schema, nodeIds));
            }
        }

        return relations;
    }

    private static Relation toRelation(FromItem from, SchemaRegistry schema, AtomicInteger nodeIds) {
        return switch (from) {
            case Table t -> {
                var tableName = t.getName();
                var alias = t.getAlias() != null ? t.getAlias().getName() : tableName;
                var attributes = schema.getAttributes(tableName);

                if (attributes.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Table '%s' not found in schema. Please register the table schema first."
                            .formatted(tableName));
                }

                yield new Relation.Table(tableName, alias, attributes);
            }
            case ParenthesedSelect ps -> {
                var alias = ps.getAlias() != null
                    ? ps.getAlias().getName()
                    : "subq" + nodeIds.getAndIncrement();
                var subqueryIR = new IRBuilder(schema, nodeIds).build((PlainSelect) ps.getSelect());
                var attributes = AttributeResolver.collectFrom(subqueryIR);
                yield new Relation.Subquery(alias, subqueryIR, attributes);
            }
            default -> throw new UnsupportedOperationException(
                "Unsupported FROM item: " + from.getClass().getSimpleName());
        };
    }
}
