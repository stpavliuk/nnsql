package nnsql.query.renderer.sql;

import nnsql.query.ir.IRExpression;

final class Format {

    private Format() {
        throw new UnsupportedOperationException("Utility class");
    }

    static String selectWhere(String columns, String from, String where) {
        return """
            SELECT %s
            FROM %s
            WHERE %s""".formatted(columns, from, where);
    }

    static String select(String columns, String from) {
        return "SELECT %s\nFROM %s".formatted(columns, from);
    }

    static String selectJoin(String columns, String fromClause) {
        return "SELECT %s\nFROM %s".formatted(columns, fromClause);
    }

    static String withSelect(String ctes, String finalSelect) {
        return """
            WITH %s
            %s
            """.formatted(ctes, finalSelect);
    }

    static String literal(IRExpression.Literal lit) {
        return switch (lit.type()) {
            case NUMBER -> lit.value().toString();
            case STRING -> "'" + lit.value().toString() + "'";
            case NULL -> "NULL";
        };
    }

    static String concatIds(String... ids) {
        return String.join(" || '_' || ", ids);
    }

    static String attrTable(String baseName, String attr) {
        return baseName + "_" + attr;
    }

    static String idTable(String baseName) {
        return baseName + "_id";
    }

    static String attrCTE(String baseName, String attr) {
        return baseName + "_attr_" + attr;
    }

    static String existsColumnToColumn(String rel, String leftCol, String rightCol, String op, boolean negate) {
        var condition = negate
            ? "NOT (%s_%s.v %s %s_%s.v)".formatted(rel, leftCol, op, rel, rightCol)
            : "%s_%s.v %s %s_%s.v".formatted(rel, leftCol, op, rel, rightCol);

        return "EXISTS (SELECT * FROM %s_%s, %s_%s WHERE %s_%s.id = %s_id.id AND %s_%s.id = %s_id.id AND %s)"
            .formatted(rel, leftCol, rel, rightCol,
                      rel, leftCol, rel,
                      rel, rightCol, rel,
                      condition);
    }

    static String existsColumnToLiteral(String rel, String col, String op, String literalValue, boolean negate) {
        var condition = negate
            ? "NOT (%s_%s.v %s %s)".formatted(rel, col, op, literalValue)
            : "%s_%s.v %s %s".formatted(rel, col, op, literalValue);

        return "EXISTS (SELECT * FROM %s_%s WHERE %s_%s.id = %s_id.id AND %s)"
            .formatted(rel, col, rel, col, rel, condition);
    }

    static String existsColumnToSubquery(String rel, String col, String op, String subquery, boolean negate) {
        var condition = negate
            ? "NOT (%s_%s.v %s (%s))".formatted(rel, col, op, subquery)
            : "%s_%s.v %s (%s)".formatted(rel, col, op, subquery);

        return "EXISTS (SELECT * FROM %s_%s WHERE %s_%s.id = %s_id.id AND %s)"
            .formatted(rel, col, rel, col, rel, condition);
    }

    static String existsLiteralToColumn(String rel, String col, String op, String literalValue, boolean negate) {
        var condition = negate
            ? "NOT (%s %s %s_%s.v)".formatted(literalValue, op, rel, col)
            : "%s %s %s_%s.v".formatted(literalValue, op, rel, col);

        return "EXISTS (SELECT * FROM %s_%s WHERE %s_%s.id = %s_id.id AND %s)"
            .formatted(rel, col, rel, col, rel, condition);
    }
}
