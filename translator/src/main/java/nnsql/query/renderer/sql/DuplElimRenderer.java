package nnsql.query.renderer.sql;

import nnsql.query.ir.DuplElim;
import nnsql.query.renderer.RenderContext;

import java.util.stream.Collectors;

class DuplElimRenderer {

    void render(DuplElim duplElim, RenderContext ctx, String baseName, String inputBaseName) {
        addIdCTE(ctx, baseName, inputBaseName, duplElim);
        addAttributeCTEs(ctx, baseName, inputBaseName, duplElim.attributes());
    }

    private void addIdCTE(RenderContext ctx, String baseName, String inputBaseName, DuplElim duplElim) {
        var equalityConditions = duplElim.attributes().stream()
            .map(attr -> generateEqualityCondition(inputBaseName, attr))
            .map(cond -> "    AND " + cond)
            .collect(Collectors.joining("\n"));

        var definition = """
            SELECT %s_id.id
            FROM %s_id
            WHERE NOT EXISTS (
              SELECT * FROM %s_id R1
              WHERE R1.id < %s_id.id
            %s
            )""".formatted(
                inputBaseName,
                inputBaseName,
                inputBaseName,
                inputBaseName,
                equalityConditions
            );

        ctx.addCTE(baseName + "_id", definition);
    }

    private void addAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                  java.util.List<String> attributes) {
        attributes.forEach(attr -> {
            var inputAttrCTE = inputBaseName + "_attr_" + attr;
            var definition = """
                SELECT %s.*
                FROM %s JOIN %s_id ON %s_id.id = %s.id""".formatted(
                    inputAttrCTE,
                    inputAttrCTE, baseName, baseName, inputAttrCTE
                );

            ctx.addCTE(baseName + "_attr_" + attr, definition);
        });
    }

    private String generateEqualityCondition(String relationName, String attr) {
        var attrTable = relationName + "_attr_" + attr;

        return """
            (EXISTS (SELECT * FROM %s TEMP1, %s TEMP2 \
            WHERE TEMP1.id = %s_id.id \
            AND TEMP2.id = R1.id AND TEMP1.v = TEMP2.v) \
            OR NOT EXISTS (SELECT * FROM %s \
            WHERE %s.id = %s_id.id \
            OR %s.id = R1.id))""".formatted(
                attrTable, attrTable,
                relationName,
                attrTable,
                attrTable, relationName,
                attrTable
            );
    }
}
