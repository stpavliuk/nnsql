package nnsql.tpch.framework;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TpchQueryDiagnostics {
    private static final int DEFAULT_ROW_SAMPLE_SIZE = 5;
    private static final int DEFAULT_CTE_SAMPLE_SIZE = 3;

    private TpchQueryDiagnostics() {
    }

    public static QueryDiagnostics collect(
        String queryName,
        String sourceSql,
        String translatedSql,
        TranslatedDbEnvironment.QueryExecution sourceExecution,
        TranslatedDbEnvironment.QueryExecution translatedExecution,
        TranslatedDbEnvironment.DatabaseHandle target
    ) {
        var cteDiagnostics = collectCteDiagnostics(translatedSql, target);
        var firstDifference = sourceExecution == null || translatedExecution == null
            ? java.util.Optional.<RowDifference>empty()
            : firstDifference(sourceExecution.rows(), translatedExecution.rows());

        return new QueryDiagnostics(
            queryName,
            sourceSql,
            translatedSql,
            sourceExecution == null ? null : sourceExecution.rows().size(),
            translatedExecution == null ? null : translatedExecution.rows().size(),
            firstRows(sourceExecution == null ? List.of() : sourceExecution.rows(), DEFAULT_ROW_SAMPLE_SIZE),
            firstRows(translatedExecution == null ? List.of() : translatedExecution.rows(), DEFAULT_ROW_SAMPLE_SIZE),
            firstDifference,
            cteDiagnostics
        );
    }

    public static String format(QueryDiagnostics diagnostics) {
        var out = new StringBuilder()
            .append("==== TPCH diagnostics for ")
            .append(diagnostics.queryName())
            .append(" ====")
            .append(System.lineSeparator())
            .append("source rows: ")
            .append(formatNullableCount(diagnostics.sourceRowCount()))
            .append(", translated rows: ")
            .append(formatNullableCount(diagnostics.translatedRowCount()))
            .append(System.lineSeparator());

        diagnostics.firstDifference().ifPresentOrElse(
            difference -> out.append("first difference: ").append(difference).append(System.lineSeparator()),
            () -> out.append("first difference: none in sampled comparable rows").append(System.lineSeparator())
        );

        appendRows(out, "source first rows", diagnostics.sourceFirstRows());
        appendRows(out, "translated first rows", diagnostics.translatedFirstRows());
        appendCtes(out, diagnostics.ctes());
        return out.toString();
    }

    private static List<CteDiagnostics> collectCteDiagnostics(
        String translatedSql,
        TranslatedDbEnvironment.DatabaseHandle target
    ) {
        if (translatedSql == null || translatedSql.isBlank() || target == null) {
            return List.of();
        }

        var ctes = TranslatedWithQuery.parse(translatedSql).ctes();
        if (ctes.isEmpty()) {
            return List.of();
        }

        var diagnostics = new ArrayList<CteDiagnostics>(ctes.size());
        for (int i = 0; i < ctes.size(); i++) {
            var visibleCtes = ctes.subList(0, i + 1);
            var cte = ctes.get(i);
            try {
                var rowCount = target.execute(countSql(visibleCtes, cte.name()));
                var sampleRows = target.execute(sampleSql(visibleCtes, cte.name()));
                diagnostics.add(new CteDiagnostics(
                    cte.name(),
                    extractLong(rowCount),
                    firstRows(sampleRows, DEFAULT_CTE_SAMPLE_SIZE),
                    null
                ));
            } catch (SQLException e) {
                diagnostics.add(new CteDiagnostics(cte.name(), null, List.of(), e.getMessage()));
            }
        }
        return List.copyOf(diagnostics);
    }

    private static String countSql(List<CteDefinition> ctes, String cteName) {
        return withSql(ctes) + " SELECT COUNT(*) FROM " + cteName;
    }

    private static String sampleSql(List<CteDefinition> ctes, String cteName) {
        return withSql(ctes) + " SELECT * FROM " + cteName + " LIMIT " + DEFAULT_CTE_SAMPLE_SIZE;
    }

    private static String withSql(List<CteDefinition> ctes) {
        var definitions = ctes.stream()
            .map(cte -> cte.name() + " AS (" + cte.body() + ")")
            .toList();
        return "WITH " + String.join(", ", definitions);
    }

    private static Long extractLong(List<List<Object>> rows) {
        if (rows.isEmpty() || rows.getFirst().isEmpty()) {
            return null;
        }
        return switch (rows.getFirst().getFirst()) {
            case Number n -> n.longValue();
            case String s -> Long.parseLong(s);
            case null -> null;
            default -> throw new IllegalStateException("Unexpected count value: " + rows.getFirst().getFirst());
        };
    }

    private static List<List<Object>> firstRows(List<List<Object>> rows, int limit) {
        return rows.stream()
            .limit(limit)
            .map(TpchQueryDiagnostics::copyRowAllowingNulls)
            .toList();
    }

    private static List<Object> copyRowAllowingNulls(List<Object> row) {
        return java.util.Collections.unmodifiableList(new ArrayList<>(row));
    }

    private static java.util.Optional<RowDifference> firstDifference(
        List<List<Object>> expected,
        List<List<Object>> actual
    ) {
        if (expected.size() != actual.size()) {
            return java.util.Optional.of(new RowDifference(
                -1,
                -1,
                "row count",
                expected.size(),
                actual.size()
            ));
        }

        var expectedSorted = sortRows(expected);
        var actualSorted = sortRows(actual);
        for (int row = 0; row < expectedSorted.size(); row++) {
            var expectedRow = expectedSorted.get(row);
            var actualRow = actualSorted.get(row);
            if (expectedRow.size() != actualRow.size()) {
                return java.util.Optional.of(new RowDifference(
                    row,
                    -1,
                    "column count",
                    expectedRow.size(),
                    actualRow.size()
                ));
            }

            for (int column = 0; column < expectedRow.size(); column++) {
                var expectedValue = expectedRow.get(column);
                var actualValue = actualRow.get(column);
                if (!Objects.equals(expectedValue, actualValue)) {
                    return java.util.Optional.of(new RowDifference(
                        row,
                        column,
                        "value",
                        expectedValue,
                        actualValue
                    ));
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static List<List<Object>> sortRows(List<List<Object>> rows) {
        var sorted = new ArrayList<>(rows);
        sorted.sort((left, right) -> {
            for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
                var comparison = compareValues(left.get(i), right.get(i));
                if (comparison != 0) {
                    return comparison;
                }
            }
            return Integer.compare(left.size(), right.size());
        });
        return sorted;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compareValues(Object left, Object right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
        }
        if (left instanceof Comparable comparable && left.getClass().isInstance(right)) {
            return comparable.compareTo(right);
        }
        return left.toString().compareTo(right.toString());
    }

    private static void appendRows(StringBuilder out, String label, List<List<Object>> rows) {
        out.append(label).append(": ");
        if (rows.isEmpty()) {
            out.append("[]").append(System.lineSeparator());
            return;
        }
        out.append(rows).append(System.lineSeparator());
    }

    private static void appendCtes(StringBuilder out, List<CteDiagnostics> ctes) {
        if (ctes.isEmpty()) {
            out.append("translated CTE probes: none").append(System.lineSeparator());
            return;
        }

        out.append("translated CTE probes:").append(System.lineSeparator());
        for (var cte : ctes) {
            out.append("  ")
                .append(cte.name())
                .append(": rows=")
                .append(formatNullableCount(cte.rowCount()));
            if (cte.error() != null && !cte.error().isBlank()) {
                out.append(", error=").append(cte.error());
            } else if (!cte.firstRows().isEmpty()) {
                out.append(", first rows=").append(cte.firstRows());
            }
            out.append(System.lineSeparator());
        }
    }

    private static String formatNullableCount(Number count) {
        return count == null ? "unknown" : count.toString();
    }

    public record QueryDiagnostics(
        String queryName,
        String sourceSql,
        String translatedSql,
        Integer sourceRowCount,
        Integer translatedRowCount,
        List<List<Object>> sourceFirstRows,
        List<List<Object>> translatedFirstRows,
        java.util.Optional<RowDifference> firstDifference,
        List<CteDiagnostics> ctes
    ) {
    }

    public record CteDiagnostics(
        String name,
        Long rowCount,
        List<List<Object>> firstRows,
        String error
    ) {
    }

    public record RowDifference(
        int rowIndex,
        int columnIndex,
        String kind,
        Object expected,
        Object actual
    ) {
    }

    record CteDefinition(String name, String body) {
    }

    record TranslatedWithQuery(List<CteDefinition> ctes) {
        static TranslatedWithQuery parse(String sql) {
            var text = stripTrailingSemicolon(sql.strip());
            if (!text.regionMatches(true, 0, "WITH", 0, 4)) {
                return new TranslatedWithQuery(List.of());
            }

            var ctes = new ArrayList<CteDefinition>();
            var cursor = 4;
            while (cursor < text.length()) {
                cursor = skipWhitespace(text, cursor);
                var nameStart = cursor;
                while (cursor < text.length() && isIdentifierChar(text.charAt(cursor))) {
                    cursor++;
                }
                if (nameStart == cursor) {
                    break;
                }
                var name = text.substring(nameStart, cursor);

                cursor = skipWhitespace(text, cursor);
                if (!text.regionMatches(true, cursor, "AS", 0, 2)) {
                    break;
                }
                cursor += 2;
                cursor = skipWhitespace(text, cursor);
                if (text.regionMatches(true, cursor, "MATERIALIZED", 0, "MATERIALIZED".length())) {
                    cursor += "MATERIALIZED".length();
                    cursor = skipWhitespace(text, cursor);
                }
                if (cursor >= text.length() || text.charAt(cursor) != '(') {
                    break;
                }

                var bodyStart = cursor + 1;
                var bodyEnd = findMatchingParen(text, cursor);
                ctes.add(new CteDefinition(name, text.substring(bodyStart, bodyEnd)));
                cursor = skipWhitespace(text, bodyEnd + 1);
                if (cursor >= text.length() || text.charAt(cursor) != ',') {
                    break;
                }
                cursor++;
            }

            return new TranslatedWithQuery(List.copyOf(ctes));
        }

        private static String stripTrailingSemicolon(String text) {
            return text.endsWith(";") ? text.substring(0, text.length() - 1) : text;
        }

        private static int skipWhitespace(String text, int cursor) {
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
                cursor++;
            }
            return cursor;
        }

        private static boolean isIdentifierChar(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
        }

        private static int findMatchingParen(String text, int openIndex) {
            var depth = 0;
            var quote = '\0';
            for (int i = openIndex; i < text.length(); i++) {
                var ch = text.charAt(i);
                if (quote != '\0') {
                    if (ch == quote) {
                        quote = '\0';
                    }
                    continue;
                }
                if (ch == '\'' || ch == '"') {
                    quote = ch;
                    continue;
                }
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
            throw new IllegalArgumentException("Unbalanced translated WITH query: " + preview(text));
        }

        private static String preview(String text) {
            return text.substring(0, Math.min(200, text.length())).replace('\n', ' ');
        }
    }
}
