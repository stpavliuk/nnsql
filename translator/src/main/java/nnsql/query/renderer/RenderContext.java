package nnsql.query.renderer;

import java.util.*;
import java.util.regex.Pattern;

public class RenderContext {
    private final Map<String, Object> state      = new HashMap<>();
    private final List<CTE>           ctes       = new ArrayList<>();
    private int                       cteCounter = 0;

    public void addCTE(String name, String definition) {
        ctes.add(new CTE(name, definition));
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
            String cteName = toVisit.poll();
            if (used.contains(cteName)) {
                continue;
            }

            used.add(cteName);
            CTE cte = cteMap.get(cteName);
            if (cte != null) {
                Set<String> referenced = findReferencedCTEs(cte.definition(), cteMap.keySet());
                for (String ref : referenced) {
                    if (!used.contains(ref)) {
                        toVisit.add(ref);
                    }
                }
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

    private Set<String> findReferencedCTEs(String definition, Set<String> allCTENames) {
        Set<String> referenced = new HashSet<>();

        for (String cteName : allCTENames) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(cteName) + "\\b");
            if (pattern.matcher(definition).find()) {
                referenced.add(cteName);
            }
        }

        return referenced;
    }

    public String nextName(String prefix) {
        return prefix + (cteCounter++);
    }

    public void put(String key, Object value) {
        state.put(key, value);
    }

    public Object get(String key) {
        return state.get(key);
    }

    public boolean contains(String key) {
        return state.containsKey(key);
    }
}
