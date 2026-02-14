package nnsql.query.builder;

import net.sf.jsqlparser.statement.select.PlainSelect;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.IRNode;

import java.util.concurrent.atomic.AtomicInteger;

public class IRBuilder {
    private final SchemaRegistry schema;
    private final AtomicInteger nodeIdCounter;

    public IRBuilder(SchemaRegistry schema) {
        this(schema, new AtomicInteger(0));
    }

    IRBuilder(SchemaRegistry schema, AtomicInteger nodeIdCounter) {
        this.schema = schema;
        this.nodeIdCounter = nodeIdCounter;
    }

    public IRNode build(PlainSelect select) {
        var analysis = SelectAnalysis.of(select);
        return IRPipeline.from(Relations.from(select, schema, nodeIdCounter), nodeIdCounter, schema)
            .filter(Conditions.from(select.getWhere(), schema, nodeIdCounter))
            .groupBy(analysis)
            .aggFilter(Conditions.from(select.getHaving(), schema, nodeIdCounter))
            .select(analysis)
            .distinctIf(select.getDistinct() != null)
            .build();
    }
}
