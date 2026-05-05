package nnsql.query.renderer;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.*;

public class RenderContext {
    private final List<CTE> ctes       = new ArrayList<>();
    private int             cteCounter = 0;

    public void addCTE(String name, PlainSelect definition) {
        ctes.add(new CTE(name, definition, dependenciesOf(definition)));
    }

    public List<CTE> getCTEs() {
        return new ArrayList<>(ctes);
    }

    public List<CTE> getUsedCTEs(Set<String> rootCTENames) {
        Map<String, CTE> cteMap = new HashMap<>();
        for (CTE cte : ctes) {
            cteMap.put(cte.name(), cte);
        }

        Set<String> used = new HashSet<>();
        Queue<String> toVisit = new LinkedList<>(rootCTENames);

        while (!toVisit.isEmpty()) {
            String name = toVisit.poll();
            if (!used.add(name)) continue;

            CTE cte = cteMap.get(name);
            if (cte != null) {
                toVisit.addAll(cte.dependencies());
            }
        }

        List<CTE> result = new ArrayList<>();
        for (CTE cte : ctes) {
            if (used.contains(cte.name())) {
                result.add(cte);
            }
        }
        return result;
    }

    public static Set<String> dependenciesOf(PlainSelect definition) {
        var dependencies = new LinkedHashSet<>(new TablesNamesFinder<>().getTables((Statement) definition));
        return Collections.unmodifiableSet(dependencies);
    }

    public String nextName(String prefix) {
        return prefix + (cteCounter++);
    }
}
