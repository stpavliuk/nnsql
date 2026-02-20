package nnsql.query.renderer;

import java.util.*;
import java.util.regex.Pattern;

public class RenderContext {
    private final List<CTE> ctes       = new ArrayList<>();
    private int             cteCounter = 0;

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
            String name = toVisit.poll();
            if (!used.add(name)) continue;

            CTE cte = cteMap.get(name);
            if (cte != null) {
                toVisit.addAll(findReferencedCTEs(cte.definition(), cteMap.keySet()));
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
}
