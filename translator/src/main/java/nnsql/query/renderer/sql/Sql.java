package nnsql.query.renderer.sql;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import nnsql.query.ir.IRExpression;
import nnsql.query.renderer.RenderContext;

import java.util.List;
import java.util.function.BiFunction;

final class Sql {

    enum NonCaseJoinMode {
        INNER_ON,
        SIMPLE_JOIN_WITH_WHERE_ID
    }

    private Sql() {
        throw new UnsupportedOperationException("Utility class");
    }

    static Table table(String name) {
        return new Table(name);
    }

    static Table tableAs(String name, String alias) {
        return new Table(name).withAlias(new Alias(alias));
    }

    static Table tableAlias(String name, String alias) {
        return new Table(name).withAlias(new Alias(alias, false));
    }

    static Column column(Table table, String col) {
        return new Column(table, col);
    }

    static Column column(String tableName, String col) {
        return new Column(new Table(tableName), col);
    }

    static Column column(String col) {
        return new Column(col);
    }

    static Expression comparison(Expression left, String op, Expression right) {
        return switch (op) {
            case "=" -> new EqualsTo(left, right);
            case "!=" -> {
                var neq = new NotEqualsTo("!=");
                neq.setLeftExpression(left);
                neq.setRightExpression(right);
                yield neq;
            }
            case "<" -> new MinorThan(left, right);
            case ">" -> new GreaterThan(left, right);
            case "<=" -> new MinorThanEquals(left, right);
            case ">=" -> new GreaterThanEquals(left, right);
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }

    static Expression and(Expression a, Expression b) {
        return new AndExpression(a, b);
    }

    static Expression andAll(List<? extends Expression> exprs) {
        return exprs.stream().map(e -> (Expression) e).reduce(Sql::and).orElseThrow();
    }

    static Expression or(Expression a, Expression b) {
        return new OrExpression(a, b);
    }

    static Expression orAll(List<? extends Expression> exprs) {
        return exprs.stream().map(e -> (Expression) e).reduce(Sql::or).orElseThrow();
    }

    static Expression paren(Expression expr) {
        return new ParenthesedExpressionList<>(expr);
    }

    static NotExpression not(Expression expr) {
        return new NotExpression(expr);
    }

    static ExistsExpression exists(PlainSelect select) {
        var sub = new ParenthesedSelect();
        sub.setSelect(select);
        var exists = new ExistsExpression();
        exists.setRightExpression(sub);
        return exists;
    }

    static ExistsExpression notExists(PlainSelect select) {
        var e = exists(select);
        e.setNot(true);
        return e;
    }

    static Expression concatSep(List<? extends Expression> exprs, String sep) {
        if (exprs.isEmpty()) throw new IllegalArgumentException("Empty expression list");
        Expression result = exprs.getFirst();
        for (int i = 1; i < exprs.size(); i++) {
            result = new Concat(new Concat(result, new StringValue(sep)), exprs.get(i));
        }
        return result;
    }

    static Expression arithmetic(Expression left, String op, Expression right) {
        return switch (op) {
            case "+" -> new Addition().withLeftExpression(left).withRightExpression(right);
            case "-" -> new Subtraction().withLeftExpression(left).withRightExpression(right);
            case "*" -> new Multiplication().withLeftExpression(left).withRightExpression(right);
            case "/" -> new Division().withLeftExpression(left).withRightExpression(right);
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }

    static Expression literal(IRExpression.Literal lit) {
        return switch (lit.type()) {
            case NUMBER -> {
                var val = lit.value();
                if (val instanceof Integer || val instanceof Long) {
                    yield new LongValue(((Number) val).longValue());
                }
                yield new DoubleValue(((Number) val).doubleValue());
            }
            case STRING -> new StringValue(lit.value().toString());
            case NULL -> new NullValue();
        };
    }

    static Function fn(String name, Expression... args) {
        return new Function(name, args);
    }

    static Join join(FromItem item, Expression on) {
        var j = new Join();
        j.setFromItem(item);
        j.addOnExpression(on);
        return j;
    }

    static Join leftJoin(FromItem item, Expression on) {
        var j = new Join();
        j.setLeft(true);
        j.setFromItem(item);
        j.addOnExpression(on);
        return j;
    }

    static Join simpleJoin(FromItem item) {
        var j = new Join();
        j.setSimple(true);
        j.setFromItem(item);
        return j;
    }

    static void addComputedExprAttributeJoins(String baseName, List<String> columns, Expression anchorIdExpr,
                                              boolean hasCaseWhen, NonCaseJoinMode nonCaseMode,
                                              List<Join> joins, List<Expression> whereConditions) {
        for (var col : columns) {
            var attrTbl = table(attrTable(baseName, col));
            var idEq = new EqualsTo(column(attrTbl, "id"), anchorIdExpr);

            if (hasCaseWhen || nonCaseMode == NonCaseJoinMode.INNER_ON) {
                joins.add(hasCaseWhen ? leftJoin(attrTbl, idEq) : join(attrTbl, idEq));
                continue;
            }

            joins.add(simpleJoin(attrTbl));
            whereConditions.add(idEq);
        }
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

    static void addFilterIdCTE(RenderContext ctx, String baseName, String inputBaseName, Expression where) {
        var idTbl = table(idTable(inputBaseName));
        var ps = new PlainSelect();
        ps.addSelectItem(column(idTbl, "id"));
        ps.setFromItem(idTbl);
        ps.setWhere(where);
        ctx.addCTE(idTable(baseName), ps.toString());
    }

    static void addPassthroughAttributeCTEs(RenderContext ctx, String baseName, String inputBaseName,
                                             List<String> attributes,
                                             BiFunction<String, String, String> naming) {
        attributes.forEach(attr -> {
            var inputAttrTbl = table(naming.apply(inputBaseName, attr));
            var baseIdTbl = table(idTable(baseName));
            var ps = new PlainSelect();
            ps.addSelectItem(new AllTableColumns(inputAttrTbl));
            ps.setFromItem(inputAttrTbl);
            ps.addJoins(join(baseIdTbl,
                new EqualsTo(column(baseIdTbl, "id"), column(inputAttrTbl, "id"))));
            ctx.addCTE(naming.apply(baseName, attr), ps.toString());
        });
    }

    static String withSelect(String ctes, String finalSelect) {
        return """
            WITH %s
            %s
            """.formatted(ctes, finalSelect);
    }
}
