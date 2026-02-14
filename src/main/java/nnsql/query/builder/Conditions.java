package nnsql.query.builder;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.Condition;
import nnsql.query.ir.IRExpression;
import nnsql.util.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Conditions {

    static Option<Condition> from(Expression expr, SchemaRegistry schema, AtomicInteger nodeIds) {
        if (expr == null) {
            return Option.none();
        }
        return Option.some(toCondition(expr, schema, nodeIds));
    }

    private static Condition toCondition(Expression expr, SchemaRegistry schema, AtomicInteger nodeIds) {
        return switch (expr) {
            case AndExpression and -> {
                var operands = new ArrayList<Condition>();
                flattenAnd(and, operands, schema, nodeIds);
                yield new Condition.And(operands);
            }
            case OrExpression or -> {
                var operands = new ArrayList<Condition>();
                flattenOr(or, operands, schema, nodeIds);
                yield new Condition.Or(operands);
            }
            case NotExpression not -> Condition.not(toCondition(not.getExpression(), schema, nodeIds));
            case IsNullExpression isn -> {
                var col = Expressions.from(isn.getLeftExpression(), schema, nodeIds);
                yield switch (col) {
                    case IRExpression.ColumnRef(var name) ->
                        new Condition.IsNull(name, isn.isNot());
                    default -> throw new IllegalArgumentException("IS NULL only on columns");
                };
            }
            case EqualsTo eq -> toComparison(eq, "=", schema, nodeIds);
            case NotEqualsTo neq -> toComparison(neq, "!=", schema, nodeIds);
            case GreaterThan gt -> toComparison(gt, ">", schema, nodeIds);
            case MinorThan lt -> toComparison(lt, "<", schema, nodeIds);
            case GreaterThanEquals gte -> toComparison(gte, ">=", schema, nodeIds);
            case MinorThanEquals lte -> toComparison(lte, "<=", schema, nodeIds);
            case ParenthesedExpressionList<?> p -> toCondition(p.getFirst(), schema, nodeIds);
            default -> throw new UnsupportedOperationException(
                "Unsupported condition: " + expr.getClass().getSimpleName());
        };
    }

    private static void flattenAnd(Expression expr, List<Condition> operands, SchemaRegistry schema, AtomicInteger nodeIds) {
        if (expr instanceof AndExpression and) {
            flattenAnd(and.getLeftExpression(), operands, schema, nodeIds);
            flattenAnd(and.getRightExpression(), operands, schema, nodeIds);
        } else {
            operands.add(toCondition(expr, schema, nodeIds));
        }
    }

    private static void flattenOr(Expression expr, List<Condition> operands, SchemaRegistry schema, AtomicInteger nodeIds) {
        if (expr instanceof OrExpression or) {
            flattenOr(or.getLeftExpression(), operands, schema, nodeIds);
            flattenOr(or.getRightExpression(), operands, schema, nodeIds);
        } else {
            operands.add(toCondition(expr, schema, nodeIds));
        }
    }

    private static Condition toComparison(ComparisonOperator op, String operator, SchemaRegistry schema, AtomicInteger nodeIds) {
        var left = Expressions.from(op.getLeftExpression(), schema, nodeIds);
        var right = Expressions.from(op.getRightExpression(), schema, nodeIds);
        return Condition.compare(left, operator, right);
    }
}
