package nnsql.query.builder;

import net.sf.jsqlparser.statement.select.PlainSelect;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.IRNode;

import java.util.concurrent.atomic.AtomicInteger;

public class Pipelines {

    public static IRNode buildQuery(PlainSelect select, SchemaRegistry schema, AtomicInteger nodeIds) {
        var analysis = SelectAnalysis.of(select);
        return IRPipeline.from(Relations.from(select, schema, nodeIds), nodeIds, schema)
            .filter(Conditions.from(select.getWhere(), schema, nodeIds))
            .groupBy(analysis)
            .aggFilter(Conditions.from(select.getHaving(), schema, nodeIds))
            .select(analysis)
            .distinctIf(select.getDistinct() != null)
            .build();
    }
}
