package nnsql.query.renderer.sql;

import nnsql.query.ir.IRExpression;
import nnsql.query.ir.Group;
import nnsql.query.renderer.RenderContext;

class GroupRenderer {

    void render(Group group, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName, group);
        addGroupingAttributeCTEs(ctx, baseName, inputBaseName, group.groupingAttributes());
        addAggregateCTEs(ctx, baseName, inputBaseName, group);
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName, Group group) {
        var groupingAttrs = group.groupingAttributes();

        if (groupingAttrs.isEmpty()) {
            var definition = """
                SELECT MIN(id) AS id
                FROM %s_id""".formatted(inputBaseName);
            ctx.addCTE(baseName + "_id", definition);
        } else {
            var equalityConditions = groupingAttrs
                .stream()
                .map(attr -> ("EXISTS (SELECT * FROM %s_%s a1, %s_%s a2 WHERE a1.id " +
                              "= %s_id.id AND a2.id = R1.id AND a1.v = a2.v)")
                    .formatted(inputBaseName, attr, inputBaseName, attr,
                        inputBaseName))
                .collect(java.util.stream.Collectors.joining(" AND "));

            var definition = """
                SELECT %s_id.id
                FROM %s_id
                WHERE NOT EXISTS (
                    SELECT * FROM %s_id R1
                    WHERE R1.id < %s_id.id AND %s
                )""".formatted(
                inputBaseName,
                inputBaseName,
                inputBaseName,
                inputBaseName,
                equalityConditions
            );

            ctx.addCTE(baseName + "_id", definition);
        }
    }

    private void addGroupingAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                          java.util.List<String> groupingAttributes) {
        for (var attr : groupingAttributes) {
            var definition = """
                SELECT %s_%s.*
                FROM %s_%s JOIN %s_id ON %s_id.id = %s_%s.id""".formatted(
                inputBaseName, attr,
                inputBaseName, attr, baseName, baseName, inputBaseName, attr
            );

            ctx.addCTE(baseName + "_" + attr, definition);
        }
    }

    private void addAggregateCTEs(RenderContext ctx, String baseName, String inputBaseName, Group group) {
        var groupingAttrs = group.groupingAttributes();

        for (var aggregate : group.aggregates()) {
            var functionName = aggregate.function();
            var argument = aggregate.argument();
            var alias = aggregate.alias();

            String columnName = switch (argument) {
                case IRExpression.ColumnRef(var col) -> col;
                default ->
                    throw new UnsupportedOperationException("Aggregate arguments other than column references are not" +
                                                            " yet supported");
            };

            String definition;
            if (functionName.equals("COUNT")) {
                definition = renderCountAggregate(baseName, inputBaseName, columnName, groupingAttrs);
            } else {
                definition = renderAggregate(baseName, inputBaseName, columnName, functionName, groupingAttrs);
            }

            ctx.addCTE(baseName + "_" + alias, definition);
        }
    }

    private String renderAggregate(String baseName, String inputBaseName, String columnName,
                                   String functionName, java.util.List<String> groupingAttrs) {
        if (groupingAttrs.isEmpty()) {
            return """
                SELECT %s_id.id, %s(%s_%s.v) AS v
                FROM %s_id, %s_id input_id, %s_%s
                WHERE %s_%s.id = input_id.id
                GROUP BY %s_id.id""".formatted(
                baseName, functionName, inputBaseName, columnName,
                baseName, inputBaseName, inputBaseName, columnName,
                inputBaseName, columnName,
                baseName
            );
        }

        var equalityConditions = groupingAttrs
            .stream()
            .map(attr -> ("EXISTS (SELECT * FROM %s_%s g1, %s_%s g2 WHERE g1.id = " +
                          "input_id.id AND g2.id = %s_id.id AND g1.v = g2.v)")
                .formatted(inputBaseName, attr, inputBaseName, attr, baseName))
            .collect(java.util.stream.Collectors.joining(" AND "));

        return """
            SELECT %s_id.id, %s(%s_%s.v) AS v
            FROM %s_id, %s_id input_id, %s_%s
            WHERE %s_%s.id = input_id.id AND %s
            GROUP BY %s_id.id""".formatted(
            baseName, functionName, inputBaseName, columnName,
            baseName, inputBaseName, inputBaseName, columnName,
            inputBaseName, columnName, equalityConditions,
            baseName
        );
    }

    private String renderCountAggregate(String baseName, String inputBaseName, String columnName,
                                        java.util.List<String> groupingAttrs) {
        var nonCountPart = renderAggregate(baseName, inputBaseName, columnName, "COUNT", groupingAttrs);

        String equalityConditions;
        if (groupingAttrs.isEmpty()) {
            equalityConditions = "TRUE";
        } else {
            equalityConditions = groupingAttrs
                .stream()
                .map(attr -> ("EXISTS (SELECT * FROM %s_%s g1, %s_%s g2 WHERE g1.id = " +
                              "input_id.id AND g2.id = %s_id.id AND g1.v = g2.v)")
                    .formatted(inputBaseName, attr, inputBaseName, attr, baseName))
                .collect(java.util.stream.Collectors.joining(" AND "));
        }

        return nonCountPart + """

            UNION
            SELECT %s_id.id, 0 AS v
            FROM %s_id
            WHERE NOT EXISTS (
                SELECT * FROM %s_id input_id, %s_%s
                WHERE %s_%s.id = input_id.id AND %s
            )""".formatted(
            baseName,
            baseName,
            inputBaseName, inputBaseName, columnName,
            inputBaseName, columnName, equalityConditions
        );
    }
}
