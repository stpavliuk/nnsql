package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.CastExpression;

import java.util.ArrayList;
import java.util.List;

import static nnsql.query.renderer.sql.Sql.fn;

final class ProductRowIdExpressionRenderers {

    private ProductRowIdExpressionRenderers() {
    }

    static ProductRowIdExpressionRenderer duckDbHash() {
        return (relationIdExpressions, nodeId) -> {
            var hashArgs = new ArrayList<Expression>(relationIdExpressions.size() + 1);
            hashArgs.addAll(relationIdExpressions);
            hashArgs.add(new LongValue(nodeId));
            return fn("hash", hashArgs.toArray(Expression[]::new));
        };
    }

    static ProductRowIdExpressionRenderer postgresUuidMd5() {
        return (relationIdExpressions, nodeId) -> {
            var concatArgs = new ArrayList<Expression>(relationIdExpressions.size() + 2);
            concatArgs.add(new StringValue("|"));
            relationIdExpressions.stream()
                .map(expression -> new CastExpression("CAST", expression, "TEXT"))
                .forEach(concatArgs::add);
            concatArgs.add(new StringValue(Long.toString(nodeId)));
            return new CastExpression("CAST", fn("md5", fn("concat_ws", concatArgs.toArray(Expression[]::new))), "UUID");
        };
    }
}
