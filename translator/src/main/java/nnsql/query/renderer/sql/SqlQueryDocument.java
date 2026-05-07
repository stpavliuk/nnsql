package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import nnsql.query.renderer.CTE;
import nnsql.query.renderer.RenderContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SqlQueryDocument {
    private final PlainSelect finalSelect;
    private final List<CTE> ctes;
    private final SqlDialect dialect;
    private final boolean forceInlineCtes;
    private final Map<String, Integer> referenceCounts;
    private final Set<String> aggregateBoundaryDependencies;

    private SqlQueryDocument(
        PlainSelect finalSelect,
        List<CTE> ctes,
        SqlDialect dialect,
        boolean forceInlineCtes,
        Map<String, Integer> referenceCounts,
        Set<String> aggregateBoundaryDependencies
    ) {
        this.finalSelect = finalSelect;
        this.ctes = ctes;
        this.dialect = dialect;
        this.forceInlineCtes = forceInlineCtes;
        this.referenceCounts = Map.copyOf(referenceCounts);
        this.aggregateBoundaryDependencies = Set.copyOf(aggregateBoundaryDependencies);
    }

    static SqlQueryDocument from(
        RenderContext ctx,
        PlainSelect finalSelect,
        SqlDialect dialect,
        boolean forceInlineCtes
    ) {
        var rootCTEs = RenderContext.dependenciesOf(finalSelect);
        var usedCTEs = ctx.getUsedCTEs(rootCTEs);
        return new SqlQueryDocument(
            finalSelect,
            usedCTEs,
            dialect,
            forceInlineCtes,
            referenceCounts(finalSelect, usedCTEs),
            aggregateBoundaryDependencies(usedCTEs)
        );
    }

    String toSql() {
        if (ctes.isEmpty()) {
            return finalSelect + ";";
        }

        var withSql = ctes.stream()
            .map(this::toWithItemSql)
            .collect(java.util.stream.Collectors.joining(", "));
        return "WITH %s %s;".formatted(withSql, finalSelect);
    }

    private String toWithItemSql(CTE cte) {
        var withItem = toWithItem(cte);
        var materialization = switch (materializationMode(cte)) {
            case DEFAULT -> "";
            case MATERIALIZED -> " MATERIALIZED";
            case NOT_MATERIALIZED -> " NOT MATERIALIZED";
        };
        return "%s AS%s %s".formatted(withItem.getAlias(), materialization, withItem.getSelect());
    }

    private WithItem<?> toWithItem(CTE cte) {
        var select = new ParenthesedSelect();
        select.setSelect(cte.definition());

        var withItem = new WithItem<ParenthesedSelect>();
        withItem.setAlias(new Alias(cte.name(), false));
        withItem.setSelect(select);
        return withItem;
    }

    private MaterializationMode materializationMode(CTE cte) {
        if (!dialect.materializeCommonTableExpressions()) {
            return MaterializationMode.DEFAULT;
        }
        if (forceInlineCtes) {
            return MaterializationMode.DEFAULT;
        }
        if (shouldForceOptimizerInline(cte)) {
            return MaterializationMode.NOT_MATERIALIZED;
        }
        return shouldMaterialize(cte)
            ? MaterializationMode.MATERIALIZED
            : MaterializationMode.DEFAULT;
    }

    private boolean shouldMaterialize(CTE cte) {
        return referenceCounts.getOrDefault(cte.name(), 0) > 1
            || isAggregateBoundary(cte.definition())
            || aggregateBoundaryDependencies.contains(cte.name());
    }

    private boolean shouldForceOptimizerInline(CTE cte) {
        return referenceCounts.getOrDefault(cte.name(), 0) > 1
            && cte.name().startsWith("filter_")
            && cte.name().endsWith("_id")
            && isSimpleFilteredAttributeScan(cte.definition());
    }

    private static boolean isSimpleFilteredAttributeScan(PlainSelect select) {
        return select.getFromItem() != null
            && (select.getJoins() == null || select.getJoins().isEmpty())
            && select.getWhere() != null
            && select.getGroupBy() == null
            && select.getHaving() == null
            && hasFilterAttributeProjection(select)
            && !containsAggregate(select);
    }

    private static boolean hasFilterAttributeProjection(PlainSelect select) {
        return select.getSelectItems().stream()
            .anyMatch(item -> {
                var alias = item.getAlias();
                return alias != null && alias.getName().startsWith("filter_attr_");
            });
    }

    private static boolean isAggregateBoundary(PlainSelect select) {
        return select.getGroupBy() != null || select.getHaving() != null || containsAggregate(select);
    }

    private static boolean containsAggregate(PlainSelect select) {
        return select.getSelectItems().stream()
            .map(item -> (Expression) item.getExpression())
            .anyMatch(SqlQueryDocument::containsAggregate);
    }

    private static boolean containsAggregate(Expression expression) {
        return switch (expression) {
            case null -> false;
            case Function function when function.isAllColumns() -> isAggregateFunction(function);
            case Function function -> isAggregateFunction(function)
                || function.getParameters() != null
                && function.getParameters().stream().anyMatch(SqlQueryDocument::containsAggregate);
            default -> containsAggregateFunctionName(expression.toString());
        };
    }

    private static boolean containsAggregateFunctionName(String expression) {
        var normalized = expression.toUpperCase(java.util.Locale.ROOT);
        return normalized.contains("COUNT(")
            || normalized.contains("SUM(")
            || normalized.contains("MIN(")
            || normalized.contains("MAX(")
            || normalized.contains("AVG(");
    }

    private static boolean isAggregateFunction(Function function) {
        return switch (function.getName().toUpperCase(java.util.Locale.ROOT)) {
            case "COUNT", "SUM", "MIN", "MAX", "AVG" -> true;
            default -> false;
        };
    }

    private static Map<String, Integer> referenceCounts(PlainSelect finalSelect, List<CTE> ctes) {
        var counts = new HashMap<String, Integer>();
        RenderContext.dependenciesOf(finalSelect).forEach(dependency -> counts.merge(dependency, 1, Integer::sum));
        for (var cte : ctes) {
            cte.dependencies().forEach(dependency -> counts.merge(dependency, 1, Integer::sum));
        }
        return counts;
    }

    private static Set<String> aggregateBoundaryDependencies(List<CTE> ctes) {
        var result = new HashSet<String>();
        for (var cte : ctes) {
            if (isAggregateBoundary(cte.definition())) {
                result.addAll(cte.dependencies());
            }
        }
        return result;
    }

    private enum MaterializationMode {
        DEFAULT,
        MATERIALIZED,
        NOT_MATERIALIZED
    }
}
