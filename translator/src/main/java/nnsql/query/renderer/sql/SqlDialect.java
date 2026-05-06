package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.LongValue;
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

    default Expression globalAggregateGroupId() {
        return new LongValue(0);
    }

    default boolean materializeCommonTableExpressions() {
        return false;
    }

    default boolean useLateralAttributeLookups() {
        return false;
    }

    default boolean optimizeSingleTableFilteredGroups() {
        return false;
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

            @Override
            public Expression globalAggregateGroupId() {
                return new CastExpression(
                    "CAST",
                    new StringValue("00000000-0000-0000-0000-000000000000"),
                    "UUID"
                );
            }

            @Override
            public boolean materializeCommonTableExpressions() {
                return true;
            }

            @Override
            public boolean useLateralAttributeLookups() {
                return true;
            }

            @Override
            public boolean optimizeSingleTableFilteredGroups() {
                return true;
            }
        };
    }
}
