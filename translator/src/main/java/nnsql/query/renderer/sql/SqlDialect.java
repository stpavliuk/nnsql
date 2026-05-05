package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.StringValue;

import java.util.List;

import static nnsql.query.renderer.sql.Sql.fn;

public interface SqlDialect {

    ProductRowIdExpressionRenderer productRowIdExpressionRenderer();

    String generatedIdType();

    default Expression renderFunction(String functionName, List<Expression> arguments) {
        return fn(functionName, arguments.toArray(Expression[]::new));
    }

    default Expression representativeId(Expression idExpression) {
        return fn("MIN", idExpression);
    }

    static SqlDialect duckDb() {
        return new SqlDialect() {
            @Override
            public ProductRowIdExpressionRenderer productRowIdExpressionRenderer() {
                return ProductRowIdExpressionRenderers.duckDbHash();
            }

            @Override
            public String generatedIdType() {
                return "UHUGEINT";
            }
        };
    }

    static SqlDialect postgres() {
        return new SqlDialect() {
            @Override
            public ProductRowIdExpressionRenderer productRowIdExpressionRenderer() {
                return ProductRowIdExpressionRenderers.postgresUuidMd5();
            }

            @Override
            public String generatedIdType() {
                return "UUID";
            }

            @Override
            public Expression renderFunction(String functionName, List<Expression> arguments) {
                if ("strftime".equalsIgnoreCase(functionName)
                    && arguments.size() == 2
                    && arguments.getFirst() instanceof StringValue format
                    && "%Y".equals(format.getValue())) {
                    return fn("to_char", arguments.get(1), new StringValue("YYYY"));
                }

                return SqlDialect.super.renderFunction(functionName, arguments);
            }

            @Override
            public Expression representativeId(Expression idExpression) {
                return new CastExpression("CAST", fn("MIN", new CastExpression("CAST", idExpression, "TEXT")), "UUID");
            }
        };
    }
}
