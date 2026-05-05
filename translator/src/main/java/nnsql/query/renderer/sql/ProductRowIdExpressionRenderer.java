package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;

import java.util.List;

@FunctionalInterface
public interface ProductRowIdExpressionRenderer {
    Expression render(List<Expression> relationIdExpressions, long nodeId);
}
