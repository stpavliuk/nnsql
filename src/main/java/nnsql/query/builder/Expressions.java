package nnsql.query.builder;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.IRExpression;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class Expressions {

    private static final Set<String> AGGREGATE_FUNCTIONS =
        Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

    static IRExpression from(Expression expr, SchemaRegistry schema, AtomicInteger nodeIds) {
        return switch (expr) {
            case Column col -> columnRef(col);
            case LongValue lv -> IRExpression.number((double) lv.getValue());
            case DoubleValue dv -> IRExpression.number(dv.getValue());
            case StringValue sv -> IRExpression.string(sv.getValue());
            case NullValue _ -> IRExpression.nullValue();
            case Function fn when isAggregate(fn) -> aggregate(fn, null, schema, nodeIds);
            case ParenthesedExpressionList<?> p -> from(p.getFirst(), schema, nodeIds);
            case ParenthesedSelect ps -> scalarSubquery(ps, schema, nodeIds);
            default -> throw new UnsupportedOperationException(
                "Unsupported expression: " + expr.getClass().getSimpleName());
        };
    }

    static IRExpression.ColumnRef columnRef(Column col) {
        var table = col.getTable();
        var name = (table != null && table.getName() != null)
            ? table.getName() + "_" + col.getColumnName()
            : col.getColumnName();
        return new IRExpression.ColumnRef(name);
    }

    static boolean isAggregate(Function fn) {
        return fn.getName() != null && AGGREGATE_FUNCTIONS.contains(fn.getName().toUpperCase());
    }

    static IRExpression.Aggregate aggregate(Function fn, String alias, SchemaRegistry schema, AtomicInteger nodeIds) {
        var functionName = fn.getName().toUpperCase();
        var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
            ? from(fn.getParameters().getFirst(), schema, nodeIds)
            : new IRExpression.ColumnRef("*");
        return new IRExpression.Aggregate(functionName, argument, alias);
    }

    private static IRExpression.ScalarSubquery scalarSubquery(ParenthesedSelect ps, SchemaRegistry schema, AtomicInteger nodeIds) {
        var subqueryIR = Pipelines.buildQuery((PlainSelect) ps.getSelect(), schema, nodeIds);
        return new IRExpression.ScalarSubquery(subqueryIR);
    }
}
