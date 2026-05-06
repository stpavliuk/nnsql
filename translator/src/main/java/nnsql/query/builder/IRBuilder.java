package nnsql.query.builder;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import nnsql.query.SchemaRegistry;
import nnsql.query.ir.*;
import nnsql.query.ir.Return.AttributeRef;
import nnsql.util.Option;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class IRBuilder {
    private final SchemaRegistry schema;
    private final AtomicInteger nodeIdCounter = new AtomicInteger(0);
    private final Map<String, Relation.Subquery> cteDefinitions = new LinkedHashMap<>();
    private final ArrayDeque<List<String>> scopeStack = new ArrayDeque<>();
    private final ArrayDeque<List<IRExpression.Correlation>> correlationCollectorStack = new ArrayDeque<>();

    private static final Set<String> AGGREGATE_FUNCTIONS =
        Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

    public IRBuilder(SchemaRegistry schema) {
        this.schema = schema;
    }

    public IRNode build(PlainSelect select) {
        cteDefinitions.clear();
        scopeStack.clear();
        correlationCollectorStack.clear();

        if (select.getWithItemsList() != null) {
            for (var withItem : select.getWithItemsList()) {
                var cteName = withItem.getAliasName();
                var cteBody = (PlainSelect) withItem.getSelect().getSelect();
                var cteIR = buildSelect(cteBody, false);
                var attributes = AttributeResolver.collectFrom(cteIR);
                cteDefinitions.put(cteName, new Relation.Subquery(cteName, cteIR, attributes));
            }
        }

        return buildSelect(select, true);
    }

    private IRNode buildSelect(PlainSelect select, boolean topLevel) {
        var pipeline = IRPipeline.start();

        var fromClause = extractFromClause(select);
        pipeline = pipeline.product(fromClause.relations());
        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());

        scopeStack.push(availableAttrs);
        try {
            var correlations = new ArrayList<IRExpression.Correlation>();

            var filterCondition = filterCondition(fromClause, select.getWhere());
            if (filterCondition.isSome()) {
                var condition = filterCondition.get();
                var whereSplit = splitCorrelations(condition, availableAttrs, outerScopeAttributes());
                correlations.addAll(whereSplit.correlations());

                if (whereSplit.localCondition().isSome()) {
                    var qualifiedCondition = AttributeResolver.qualifyCondition(
                        whereSplit.localCondition().get(),
                        availableAttrs
                    );
                    pipeline = pipeline.filter(qualifiedCondition, availableAttrs);
                }
            }

            boolean hasAggregates = hasAggregatesInSelect(select);
            boolean hasGroupBy = select.getGroupBy() != null;
            var deduplicatedCorrelations = deduplicateCorrelations(correlations);
            GroupedQueryPreparation groupedQuery = null;

            if (!deduplicatedCorrelations.isEmpty()) {
                if (correlationCollectorStack.isEmpty()) {
                    throw new UnsupportedOperationException(
                        "Correlated subqueries are not supported in this context"
                    );
                }
                correlationCollectorStack.peek().addAll(deduplicatedCorrelations);
            }

            if (hasGroupBy || hasAggregates) {
                var correlationGroupingAttrs = deduplicatedCorrelations.stream()
                    .map(IRExpression.Correlation::innerAttribute)
                    .toList();
                groupedQuery = prepareGroupedQuery(select, availableAttrs, correlationGroupingAttrs);
                pipeline = pipeline.group(
                    groupedQuery.groupingAttributes(),
                    groupedQuery.aggregates(),
                    groupedQuery.outputAttributes()
                );
            }

            if (select.getHaving() != null) {
                var attributes = AttributeResolver.collectFrom(pipeline.build());
                var condition = groupedQuery != null
                    ? groupedQuery.havingCondition().orElseThrow(() -> new IllegalStateException(
                        "Grouped query analysis is missing HAVING condition"
                    ))
                    : AttributeResolver.qualifyCondition(toCondition(select.getHaving()), attributes);
                pipeline = pipeline.aggFilter(condition, attributes);
            }

            if (hasGroupBy || hasAggregates) {
                pipeline = groupedQuery.selectStar()
                    ? pipeline.returnAll()
                    : pipeline.returnSelected(groupedQuery.selectedAttributes());
            } else {
                pipeline = buildReturnForNonGroupBy(select, pipeline);
            }

            if (select.getDistinct() != null) {
                var attributes = AttributeResolver.collectFrom(pipeline.build());
                pipeline = pipeline.duplElim(attributes);
            }

            if (topLevel) {
                var sortKeys = parseSortKeys(select, pipeline);
                var limit = parseLimit(select);
                if (!sortKeys.isEmpty() || limit != null) {
                    pipeline = pipeline.sort(sortKeys, limit);
                }
            } else if ((select.getOrderByElements() != null && !select.getOrderByElements().isEmpty())
                || select.getLimit() != null) {
                throw new UnsupportedOperationException(
                    "ORDER BY/LIMIT in subqueries or CTEs is not supported");
            }

            return pipeline.build();
        } finally {
            scopeStack.pop();
        }
    }

    private FromClause extractFromClause(PlainSelect select) {
        var relations = new ArrayList<Relation>();
        var joinConditions = new ArrayList<Condition>();
        relations.add(toRelation(select.getFromItem()));

        if (select.getJoins() != null) {
            for (var join : select.getJoins()) {
                validateSupportedJoin(join);
                relations.add(toRelation(join.getFromItem()));
                collectJoinConditions(join, joinConditions);
            }
        }

        return new FromClause(relations, joinConditions);
    }

    private void validateSupportedJoin(Join join) {
        if (join.isSimple() || join.isCross() || join.isInner() || join.isInnerJoin()) {
            return;
        }

        if (join.isLeft() || join.isRight() || join.isFull() || join.isOuter()) {
            throw new UnsupportedOperationException(
                "Outer JOIN syntax is not supported yet; it needs row-preserving 6NF semantics"
            );
        }

        if (join.isNatural()) {
            throw new UnsupportedOperationException("NATURAL JOIN syntax is not supported yet");
        }

        if (join.getUsingColumns() != null && !join.getUsingColumns().isEmpty()) {
            throw new UnsupportedOperationException("JOIN ... USING syntax is not supported yet");
        }

        throw new UnsupportedOperationException("Unsupported JOIN syntax: " + join);
    }

    private void collectJoinConditions(Join join, List<Condition> joinConditions) {
        if (join.getUsingColumns() != null && !join.getUsingColumns().isEmpty()) {
            throw new UnsupportedOperationException("JOIN ... USING syntax is not supported yet");
        }

        var onExpressions = join.getOnExpressions();
        if (onExpressions == null) {
            return;
        }

        onExpressions.stream()
            .map(this::toCondition)
            .forEach(joinConditions::add);
    }

    private Option<Condition> filterCondition(FromClause fromClause, Expression where) {
        var conditions = new ArrayList<Condition>(fromClause.joinConditions());
        if (where != null) {
            conditions.add(toCondition(where));
        }

        return switch (conditions.size()) {
            case 0 -> Option.none();
            case 1 -> Option.some(conditions.getFirst());
            default -> Option.some((Condition) Condition.and(conditions));
        };
    }

    private Relation toRelation(FromItem from) {
        return switch (from) {
            case Table t -> {
                var tableName = t.getName();
                var alias = Option.ofNullable(t.getAlias())
                    .map(Alias::getName)
                    .orElse(tableName);

                var cteDef = cteDefinitions.get(tableName);
                if (cteDef != null) {
                    yield new Relation.Subquery(alias, cteDef.ir(), cteDef.attributes());
                }

                var attributes = schema.getAttributes(tableName);

                if (attributes.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Table '%s' not found in schema. Please register the table schema first."
                            .formatted(tableName));
                }

                yield new Relation.Table(tableName, alias, attributes);
            }
            case ParenthesedSelect ps -> {
                var alias = Option.ofNullable(ps.getAlias())
                    .map(Alias::getName)
                    .orElseGet(() -> "subq" + nodeIdCounter.getAndIncrement());
                var subqueryIR = buildSelect((PlainSelect) ps.getSelect(), false);
                var attributes = AttributeResolver.collectFrom(subqueryIR);
                yield new Relation.Subquery(alias, subqueryIR, attributes);
            }
            default -> throw new UnsupportedOperationException(
                "Unsupported FROM item: " + from.getClass().getSimpleName());
        };
    }

    IRExpression toExpression(Expression expr) {
        return switch (expr) {
            case Column col -> toColumnRef(col);
            case LongValue lv -> IRExpression.number((double) lv.getValue());
            case DoubleValue dv -> IRExpression.number(dv.getValue());
            case StringValue sv -> IRExpression.string(sv.getValue());
            case NullValue _ -> IRExpression.nullValue();
            case Function fn when isAggregate(fn) -> toAggregate(fn, null);
            case Function fn -> toFunctionCall(fn);
            case CaseExpression caseExpr -> {
                var switchExpr = caseExpr.getSwitchExpression();
                var whens = caseExpr.getWhenClauses().stream()
                    .map(whenClause -> {
                        Condition condition;
                        if (switchExpr != null) {
                            condition = Condition.eq(
                                toExpression(switchExpr),
                                toExpression(whenClause.getWhenExpression())
                            );
                        } else {
                            condition = toCondition(whenClause.getWhenExpression());
                        }
                        return new IRExpression.WhenClause(
                            condition,
                            toExpression(whenClause.getThenExpression())
                        );
                    })
                    .toList();
                var elseExpr = Option.ofNullable(caseExpr.getElseExpression())
                    .map(this::toExpression);
                yield new IRExpression.CaseWhen(whens, elseExpr);
            }
            case ParenthesedExpressionList<?> p -> toExpression(p.getFirst());
            case ParenthesedSelect ps -> toScalarSubquery(ps);
            case Addition add -> new IRExpression.BinaryOp(
                toExpression(add.getLeftExpression()), "+", toExpression(add.getRightExpression()));
            case Subtraction sub -> new IRExpression.BinaryOp(
                toExpression(sub.getLeftExpression()), "-", toExpression(sub.getRightExpression()));
            case Multiplication mul -> new IRExpression.BinaryOp(
                toExpression(mul.getLeftExpression()), "*", toExpression(mul.getRightExpression()));
            case Division div -> new IRExpression.BinaryOp(
                toExpression(div.getLeftExpression()), "/", toExpression(div.getRightExpression()));
            case CastExpression cast ->
                new IRExpression.Cast(
                    toExpression(cast.getLeftExpression()),
                    cast.getColDataType().toString()
                );
            default -> throw new UnsupportedOperationException(
                "Unsupported expression: " + expr.getClass().getSimpleName());
        };
    }

    private IRExpression.ColumnRef toColumnRef(Column col) {
        var table = col.getTable();
        var name = (table != null && table.getName() != null)
            ? table.getName() + "_" + col.getColumnName()
            : col.getColumnName();
        return new IRExpression.ColumnRef(name);
    }

    private boolean isAggregate(Function fn) {
        return fn.getName() != null && AGGREGATE_FUNCTIONS.contains(fn.getName().toUpperCase());
    }

    private IRExpression.Aggregate toAggregate(Function fn, String alias) {
        var functionName = fn.getName().toUpperCase();
        var argument = fn.getParameters() != null && !fn.getParameters().isEmpty()
                && !(fn.getParameters().getFirst() instanceof AllColumns)
            ? toExpression(fn.getParameters().getFirst())
            : IRExpression.number(1);
        return new IRExpression.Aggregate(functionName, argument, alias, fn.isDistinct());
    }

    private IRExpression.FunctionCall toFunctionCall(Function fn) {
        if (fn.isDistinct()) {
            throw new UnsupportedOperationException("DISTINCT in scalar functions is not supported");
        }

        var name = Option.ofNullable(fn.getName())
            .map(String::strip)
            .flatMap(value -> value.isEmpty() ? Option.none() : Option.some(value))
            .orElseThrow(() -> new UnsupportedOperationException("Unnamed function is not supported"));

        var arguments = Option.ofNullable(fn.getParameters())
            .map(parameters -> parameters.stream()
                .map(Expression.class::cast)
                .map(param -> switch (param) {
                    case AllColumns _ -> throw new UnsupportedOperationException(
                        "Scalar functions with * arguments are not supported"
                    );
                    default -> toExpression(param);
                })
                .toList())
            .orElse(List.of());

        return new IRExpression.FunctionCall(name, arguments);
    }

    private IRExpression.ScalarSubquery toScalarSubquery(ParenthesedSelect ps) {
        var correlations = new ArrayList<IRExpression.Correlation>();
        correlationCollectorStack.push(correlations);
        try {
            var fullSubqueryIR = buildSelect((PlainSelect) ps.getSelect(), false);
            var deduplicatedCorrelations = deduplicateCorrelations(correlations);
            var valueAttribute = deduplicatedCorrelations.isEmpty()
                ? Option.<String>none()
                : extractScalarSubqueryValueAttribute(fullSubqueryIR);

            var rootNode = deduplicatedCorrelations.isEmpty()
                ? fullSubqueryIR
                : buildCorrelatedScalarSubqueryRoot(fullSubqueryIR, deduplicatedCorrelations, valueAttribute);

            var pipeline = new ArrayList<IRNode>();
            pipeline.addFirst(rootNode);
            return new IRExpression.ScalarSubquery(
                pipeline,
                deduplicatedCorrelations,
                valueAttribute
            );
        } finally {
            correlationCollectorStack.pop();
        }
    }

    private IRNode buildCorrelatedScalarSubqueryRoot(
        IRNode fullSubqueryIR,
        List<IRExpression.Correlation> correlations,
        Option<String> valueAttribute
    ) {
        findGroupNode(fullSubqueryIR).orElseThrow(() -> new UnsupportedOperationException(
            "Correlated scalar subqueries require an aggregate projection"
        ));

        var returnNode = findReturnNode(fullSubqueryIR).orElseThrow(() -> new UnsupportedOperationException(
            "Correlated scalar subqueries require a scalar projection"
        ));
        if (returnNode.selectStar() || returnNode.selectedAttributes().size() != 1 || valueAttribute.isNone()) {
            throw new UnsupportedOperationException(
                "Correlated scalar subqueries require a scalar projection"
            );
        }

        var selectedAttributes = new ArrayList<Return.AttributeRef>();
        selectedAttributes.add(returnNode.selectedAttributes().getFirst());
        correlations.stream()
            .map(IRExpression.Correlation::innerAttribute)
            .distinct()
            .filter(attr -> !attr.equals(valueAttribute.get()))
            .map(attr -> Return.AttributeRef.attr(attr, attr))
            .forEach(selectedAttributes::add);

        return new Return(returnNode.input(), selectedAttributes, false);
    }

    private Option<Group> findGroupNode(IRNode node) {
        return switch (node) {
            case Group g -> Option.some(g);
            case Sort s -> findGroupNode(s.input());
            case DuplElim d -> findGroupNode(d.input());
            case Return r -> findGroupNode(r.input());
            case AggFilter af -> findGroupNode(af.input());
            case Filter f -> findGroupNode(f.input());
            case Product _ -> Option.none();
        };
    }

    private Option<String> extractScalarSubqueryValueAttribute(IRNode subqueryRoot) {
        return switch (findReturnNode(subqueryRoot)) {
            case Option.Some(var returnNode) when !returnNode.selectStar()
                && returnNode.selectedAttributes().size() == 1 ->
                Option.some(returnNode.selectedAttributes().getFirst().alias());
            default -> Option.none();
        };
    }

    private Option<Return> findReturnNode(IRNode node) {
        return switch (node) {
            case Return r -> Option.some(r);
            case Sort s -> findReturnNode(s.input());
            case DuplElim d -> findReturnNode(d.input());
            case AggFilter af -> findReturnNode(af.input());
            case Group g -> findReturnNode(g.input());
            case Filter f -> findReturnNode(f.input());
            case Product _ -> Option.none();
        };
    }

    Condition toCondition(Expression expr) {
        return switch (expr) {
            case AndExpression and -> {
                var operands = new ArrayList<Condition>();
                flattenAnd(and, operands);
                yield new Condition.And(operands);
            }
            case OrExpression or -> {
                var operands = new ArrayList<Condition>();
                flattenOr(or, operands);
                yield new Condition.Or(operands);
            }
            case NotExpression not -> Condition.not(toCondition(not.getExpression()));
            case IsNullExpression isn -> {
                var col = toExpression(isn.getLeftExpression());
                yield switch (col) {
                    case IRExpression.ColumnRef(var name) ->
                        new Condition.IsNull(name, isn.isNot());
                    default -> throw new IllegalArgumentException("IS NULL only on columns");
                };
            }
            case EqualsTo eq -> toComparison(eq, "=");
            case NotEqualsTo neq -> toComparison(neq, "!=");
            case GreaterThan gt -> toComparison(gt, ">");
            case MinorThan lt -> toComparison(lt, "<");
            case GreaterThanEquals gte -> toComparison(gte, ">=");
            case MinorThanEquals lte -> toComparison(lte, "<=");
            case LikeExpression like -> toLikeCondition(like);
            case ExistsExpression exists -> toExistsCondition(exists);
            case Between between -> {
                var left = toExpression(between.getLeftExpression());
                var start = toExpression(between.getBetweenExpressionStart());
                var end = toExpression(between.getBetweenExpressionEnd());
                yield between.isNot()
                    ? Condition.or(Condition.lt(left, start), Condition.gt(left, end))
                    : Condition.and(Condition.gte(left, start), Condition.lte(left, end));
            }
            case InExpression in -> toInCondition(in);
            case ParenthesedExpressionList<?> p -> toCondition(p.getFirst());
            default -> throw new UnsupportedOperationException(
                "Unsupported condition: " + expr.getClass().getSimpleName());
        };
    }

    private Condition toInCondition(InExpression in) {
        var normalized = normalizeInRightExpression(in.getRightExpression());
        var inPayloadCondition = switch (normalized.payload()) {
            case ExpressionList<?> list -> toInListCondition(in, list);
            case ParenthesedSelect subquery -> toInSubqueryCondition(in, subquery);
            default -> throw new UnsupportedOperationException(
                "IN supports value lists and subqueries only"
            );
        };

        if (normalized.trailingPredicates().isEmpty()) {
            return inPayloadCondition;
        }

        var operands = new ArrayList<Condition>();
        operands.add(inPayloadCondition);
        normalized.trailingPredicates().forEach(expr -> operands.add(toCondition(expr)));
        return Condition.and(operands);
    }

    private Condition toInListCondition(InExpression in, ExpressionList<?> list) {
        if (list.isEmpty()) {
            throw new UnsupportedOperationException("IN with empty list is not supported");
        }

        var left = toExpression(in.getLeftExpression());
        var values = list.stream()
            .map(Expression.class::cast)
            .map(this::toExpression)
            .map(value -> switch (value) {
                case IRExpression.Literal lit -> lit;
                default -> throw new UnsupportedOperationException(
                    "IN list supports literal values only"
                );
            })
            .toList();

        return in.isNot()
            ? Condition.and(values.stream().map(v -> (Condition) Condition.neq(left, v)).toList())
            : Condition.or(values.stream().map(v -> (Condition) Condition.eq(left, v)).toList());
    }

    private Condition toExistsCondition(ExistsExpression exists) {
        var rightExpr = exists.getRightExpression();
        if (!(rightExpr instanceof ParenthesedSelect ps)) {
            throw new UnsupportedOperationException("EXISTS supports subqueries only");
        }

        var correlations = new ArrayList<IRExpression.Correlation>();
        correlationCollectorStack.push(correlations);
        try {
            var subqueryIR = buildSelect((PlainSelect) ps.getSelect(), false);
            var deduplicatedCorrelations = deduplicateCorrelations(correlations);
            if (deduplicatedCorrelations.isEmpty()) {
                return exists.isNot()
                    ? Condition.notExists(subqueryIR)
                    : Condition.exists(subqueryIR);
            }
            return exists.isNot()
                ? Condition.correlatedNotExists(subqueryIR, deduplicatedCorrelations)
                : Condition.correlatedExists(subqueryIR, deduplicatedCorrelations);
        } finally {
            correlationCollectorStack.pop();
        }
    }

    private Condition toInSubqueryCondition(InExpression in, ParenthesedSelect subquery) {
        var left = toExpression(in.getLeftExpression());
        var subqueryIR = buildSelect((PlainSelect) subquery.getSelect(), false);
        return in.isNot()
            ? Condition.notInSubquery(left, subqueryIR)
            : Condition.inSubquery(left, subqueryIR);
    }

    private InRightExpression normalizeInRightExpression(Expression rightExpression) {
        if (!(rightExpression instanceof AndExpression and)) {
            return new InRightExpression(rightExpression, List.of());
        }

        var operands = new ArrayList<Expression>();
        flattenAndExpressions(and, operands);
        var payload = operands.getFirst();
        var trailingPredicates = new ArrayList<Expression>();
        if (operands.size() > 1) {
            trailingPredicates.addAll(operands.subList(1, operands.size()));
        }
        return new InRightExpression(payload, trailingPredicates);
    }

    private void flattenAndExpressions(Expression expr, List<Expression> operands) {
        if (expr instanceof AndExpression and) {
            flattenAndExpressions(and.getLeftExpression(), operands);
            flattenAndExpressions(and.getRightExpression(), operands);
        } else {
            operands.add(expr);
        }
    }

    private record InRightExpression(Expression payload, List<Expression> trailingPredicates) {
    }

    private record FromClause(List<Relation> relations, List<Condition> joinConditions) {
        FromClause {
            relations = List.copyOf(relations);
            joinConditions = List.copyOf(joinConditions);
        }
    }

    private record CorrelationSplit(Option<Condition> localCondition, List<IRExpression.Correlation> correlations) {
    }

    private enum ResolvedScope {
        NONE,
        LOCAL,
        OUTER,
        MIXED,
        UNRESOLVED
    }

    private record ResolvedColumnRef(String attribute, ResolvedScope scope) {
    }

    private List<String> outerScopeAttributes() {
        if (scopeStack.size() <= 1) {
            return List.of();
        }

        var attributes = new ArrayList<String>();
        var iterator = scopeStack.iterator();
        if (iterator.hasNext()) {
            iterator.next();
        }
        while (iterator.hasNext()) {
            attributes.addAll(iterator.next());
        }
        return attributes;
    }

    private CorrelationSplit splitCorrelations(
        Condition condition,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        if (outerAttrs.isEmpty()) {
            return new CorrelationSplit(Option.some(condition), List.of());
        }

        var operands = flattenAndConditions(condition);
        var localOperands = new ArrayList<Condition>();
        var correlations = new ArrayList<IRExpression.Correlation>();

        for (var operand : operands) {
            if (tryExtractCorrelation(operand, localAttrs, outerAttrs)
                .map(correlation -> {
                    correlations.add(correlation);
                    return true;
                }).orElse(false)) {
                continue;
            }

            if (usesOuterScope(operand, localAttrs, outerAttrs)) {
                throw new UnsupportedOperationException(
                    "Unsupported correlated predicate: " + operand
                );
            }
            localOperands.add(operand);
        }

        Option<Condition> localCondition = switch (localOperands.size()) {
            case 0 -> Option.none();
            case 1 -> Option.some(localOperands.getFirst());
            default -> Option.some((Condition) Condition.and(localOperands));
        };

        return new CorrelationSplit(localCondition, correlations);
    }

    private List<Condition> flattenAndConditions(Condition condition) {
        return switch (condition) {
            case Condition.And(var operands) -> operands.stream()
                .flatMap(operand -> flattenAndConditions(operand).stream())
                .toList();
            default -> List.of(condition);
        };
    }

    private Option<IRExpression.Correlation> tryExtractCorrelation(
        Condition condition,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        return switch (condition) {
            case Condition.Comparison(
                IRExpression.ColumnRef(var left),
                IRExpression.ColumnRef(var right),
                var operator
            ) -> {
                var resolvedLeft = resolveColumn(left, localAttrs, outerAttrs);
                var resolvedRight = resolveColumn(right, localAttrs, outerAttrs);
                if (resolvedLeft.scope() == ResolvedScope.LOCAL && resolvedRight.scope() == ResolvedScope.OUTER) {
                    yield Option.some(new IRExpression.Correlation(
                        resolvedRight.attribute(),
                        resolvedLeft.attribute(),
                        operator
                    ));
                }
                if (resolvedLeft.scope() == ResolvedScope.OUTER && resolvedRight.scope() == ResolvedScope.LOCAL) {
                    yield Option.some(new IRExpression.Correlation(
                        resolvedLeft.attribute(),
                        resolvedRight.attribute(),
                        flipOperator(operator)
                    ));
                }
                yield Option.none();
            }
            default -> Option.none();
        };
    }

    private String flipOperator(String operator) {
        return switch (operator) {
            case "<" -> ">";
            case ">" -> "<";
            case "<=" -> ">=";
            case ">=" -> "<=";
            default -> operator;
        };
    }

    private boolean usesOuterScope(
        Condition condition,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        return switch (conditionScope(condition, localAttrs, outerAttrs)) {
            case OUTER, MIXED -> true;
            case NONE, LOCAL -> false;
            case UNRESOLVED -> throw new IllegalArgumentException(
                "Unable to resolve columns in correlated predicate: " + condition
            );
        };
    }

    private ResolvedScope conditionScope(
        Condition condition,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, _) ->
                mergeScope(expressionScope(left, localAttrs, outerAttrs),
                    expressionScope(right, localAttrs, outerAttrs));
            case Condition.IsNull(var attr, _) ->
                resolveScope(attr, localAttrs, outerAttrs);
            case Condition.Like(var left, var pattern, _) ->
                mergeScope(
                    expressionScope(left, localAttrs, outerAttrs),
                    expressionScope(pattern, localAttrs, outerAttrs)
                );
            case Condition.Exists(var subquery, _, _) ->
                conditionScopeForSubquery(subquery, localAttrs, outerAttrs);
            case Condition.InSubquery(_, var subquery, _) ->
                conditionScopeForSubquery(subquery, localAttrs, outerAttrs);
            case Condition.And(var operands) -> operands.stream()
                .map(operand -> conditionScope(operand, localAttrs, outerAttrs))
                .reduce(ResolvedScope.NONE, this::mergeScope);
            case Condition.Or(var operands) -> operands.stream()
                .map(operand -> conditionScope(operand, localAttrs, outerAttrs))
                .reduce(ResolvedScope.NONE, this::mergeScope);
            case Condition.Not(var operand) ->
                conditionScope(operand, localAttrs, outerAttrs);
        };
    }

    private ResolvedScope conditionScopeForSubquery(
        IRNode subquery,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        return ResolvedScope.NONE;
    }

    private ResolvedScope expressionScope(
        IRExpression expression,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        return switch (expression) {
            case IRExpression.ColumnRef(var attr) -> resolveScope(attr, localAttrs, outerAttrs);
            case IRExpression.Literal _ -> ResolvedScope.NONE;
            case IRExpression.BinaryOp(var left, _, var right) ->
                mergeScope(
                    expressionScope(left, localAttrs, outerAttrs),
                    expressionScope(right, localAttrs, outerAttrs)
                );
            case IRExpression.Cast(var inner, _) ->
                expressionScope(inner, localAttrs, outerAttrs);
            case IRExpression.FunctionCall(_, var arguments) -> arguments.stream()
                .map(argument -> expressionScope(argument, localAttrs, outerAttrs))
                .reduce(ResolvedScope.NONE, this::mergeScope);
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var whenScope = whens.stream()
                    .map(when -> mergeScope(
                        conditionScope(when.condition(), localAttrs, outerAttrs),
                        expressionScope(when.result(), localAttrs, outerAttrs)
                    ))
                    .reduce(ResolvedScope.NONE, this::mergeScope);
                var elseScope = elseExpr.stream()
                    .map(elseValue -> expressionScope(elseValue, localAttrs, outerAttrs))
                    .reduce(ResolvedScope.NONE, this::mergeScope);
                yield mergeScope(whenScope, elseScope);
            }
            case IRExpression.Aggregate(var _, var argument, _, _) ->
                expressionScope(argument, localAttrs, outerAttrs);
            case IRExpression.ScalarSubquery _ -> ResolvedScope.NONE;
        };
    }

    private ResolvedColumnRef resolveColumn(
        String attr,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        return switch (resolveScope(attr, localAttrs, outerAttrs)) {
            case LOCAL -> new ResolvedColumnRef(AttributeResolver.resolve(attr, localAttrs), ResolvedScope.LOCAL);
            case OUTER -> new ResolvedColumnRef(AttributeResolver.resolve(attr, outerAttrs), ResolvedScope.OUTER);
            case NONE -> new ResolvedColumnRef(attr, ResolvedScope.NONE);
            case MIXED -> new ResolvedColumnRef(attr, ResolvedScope.MIXED);
            case UNRESOLVED -> new ResolvedColumnRef(attr, ResolvedScope.UNRESOLVED);
        };
    }

    private ResolvedScope resolveScope(
        String attr,
        List<String> localAttrs,
        List<String> outerAttrs
    ) {
        var inLocal = canResolve(attr, localAttrs);
        if (inLocal) {
            return ResolvedScope.LOCAL;
        }

        var inOuter = canResolve(attr, outerAttrs);
        if (inOuter) {
            return ResolvedScope.OUTER;
        }
        return ResolvedScope.UNRESOLVED;
    }

    private boolean canResolve(String attr, List<String> availableAttrs) {
        if (availableAttrs.contains(attr)) {
            return true;
        }

        var suffix = "_" + attr;
        return availableAttrs.stream().anyMatch(candidate -> candidate.endsWith(suffix));
    }

    private ResolvedScope mergeScope(ResolvedScope left, ResolvedScope right) {
        if (left == ResolvedScope.UNRESOLVED || right == ResolvedScope.UNRESOLVED) {
            return ResolvedScope.UNRESOLVED;
        }
        if (left == ResolvedScope.NONE) {
            return right;
        }
        if (right == ResolvedScope.NONE) {
            return left;
        }
        if (left == right) {
            return left;
        }
        return ResolvedScope.MIXED;
    }

    private List<IRExpression.Correlation> deduplicateCorrelations(List<IRExpression.Correlation> correlations) {
        return new ArrayList<>(new LinkedHashSet<>(correlations));
    }

    private void flattenAnd(Expression expr, List<Condition> operands) {
        if (expr instanceof AndExpression and) {
            flattenAnd(and.getLeftExpression(), operands);
            flattenAnd(and.getRightExpression(), operands);
        } else {
            operands.add(toCondition(expr));
        }
    }

    private void flattenOr(Expression expr, List<Condition> operands) {
        if (expr instanceof OrExpression or) {
            flattenOr(or.getLeftExpression(), operands);
            flattenOr(or.getRightExpression(), operands);
        } else {
            operands.add(toCondition(expr));
        }
    }

    private Condition toComparison(ComparisonOperator op, String operator) {
        var left = toExpression(op.getLeftExpression());
        var right = toExpression(op.getRightExpression());
        return Condition.compare(left, operator, right);
    }

    private Condition toLikeCondition(LikeExpression like) {
        if (like.getEscape() != null) {
            throw new UnsupportedOperationException("LIKE ... ESCAPE is not supported");
        }
        if (like.getLikeKeyWord() == LikeExpression.KeyWord.ILIKE) {
            throw new UnsupportedOperationException("Case-insensitive LIKE is not supported");
        }

        var left = toExpression(like.getLeftExpression());
        var pattern = toExpression(like.getRightExpression());
        return like.isNot()
            ? Condition.notLike(left, pattern)
            : Condition.like(left, pattern);
    }

    private boolean hasAggregatesInSelect(PlainSelect select) {
        for (var item : select.getSelectItems()) {
            if (item.getExpression() instanceof AllColumns) {
                continue;
            }
            if (containsAggregate(toExpression(item.getExpression()))) {
                return true;
            }
        }
        return false;
    }

    private GroupedQueryPreparation prepareGroupedQuery(
        PlainSelect select,
        List<String> availableAttrs,
        List<String> extraGroupingAttributes
    ) {
        var selectAliasSourceAttributes = new LinkedHashMap<String, String>();
        var selectAliasUnsupportedExpressionTypes = new LinkedHashMap<String, String>();
        buildGroupByAliasBindings(
            select.getSelectItems(),
            availableAttrs,
            selectAliasSourceAttributes,
            selectAliasUnsupportedExpressionTypes
        );

        List<String> groupingAttributes;
        if (select.getGroupBy() != null) {
            groupingAttributes = resolveGroupByAttributes(
                select,
                availableAttrs,
                selectAliasSourceAttributes,
                selectAliasUnsupportedExpressionTypes
            );
        } else {
            groupingAttributes = new ArrayList<>();
        }

        for (var attr : extraGroupingAttributes) {
            if (!groupingAttributes.contains(attr)) {
                groupingAttributes.add(attr);
            }
        }

        var collector = new AggregateCollector(availableAttrs);
        var selectedAttrs = new ArrayList<AttributeRef>();
        var selectItems = select.getSelectItems();
        boolean selectStar = isSelectAll(selectItems);

        if (!selectStar) {
            for (int i = 0; i < selectItems.size(); i++) {
                buildGroupedAttributeRef(selectItems.get(i), availableAttrs, i + 1, collector)
                    .stream()
                    .forEach(selectedAttrs::add);
            }
        }

        var outputAttributes = new ArrayList<>(groupingAttributes);
        outputAttributes.addAll(collector.aggregates().stream()
            .map(IRExpression.Aggregate::alias)
            .filter(alias -> !outputAttributes.contains(alias))
            .toList());

        var havingCondition = Option.ofNullable(select.getHaving())
            .map(this::toCondition)
            .map(condition -> {
                var rewrittenCondition = rewriteAggregateCondition(condition, collector);
                var havingAvailableAttrs = new ArrayList<>(availableAttrs);
                collector.aggregates().stream()
                    .map(IRExpression.Aggregate::alias)
                    .filter(alias -> !havingAvailableAttrs.contains(alias))
                    .forEach(havingAvailableAttrs::add);
                return qualifyGroupedCondition(rewrittenCondition, havingAvailableAttrs);
            });

        return new GroupedQueryPreparation(
            groupingAttributes,
            collector.aggregates(),
            outputAttributes,
            havingCondition,
            selectedAttrs,
            selectStar
        );
    }

    private IRPipeline buildReturnForNonGroupBy(PlainSelect select, IRPipeline pipeline) {
        var selectItems = select.getSelectItems();

        if (isSelectAll(selectItems)) {
            return pipeline.returnAll();
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectedAttrs = IntStream.range(0, selectItems.size())
            .mapToObj(i -> buildNonGroupByAttributeRef(selectItems.get(i), availableAttrs, i + 1))
            .flatMap(Option::stream)
            .toList();

        return pipeline.returnSelected(selectedAttrs);
    }

    private Option<AttributeRef> buildNonGroupByAttributeRef(
        SelectItem<?> item,
        List<String> availableAttrs,
        int position
    ) {
        if (item.getExpression() instanceof AllColumns) {
            return Option.none();
        }
        return Option.some(buildAttributeRef(item, availableAttrs, position));
    }

    private AttributeRef buildAttributeRef(SelectItem<?> item, List<String> availableAttrs, int position) {
        var irExpr = toExpression(item.getExpression());

        return switch (irExpr) {
            case IRExpression.ColumnRef(var colName) -> {
                var resolvedName = AttributeResolver.resolve(colName, availableAttrs);
                var alias = selectItemAlias(item, OutputAlias.column(colName));
                yield AttributeRef.attr(resolvedName, alias);
            }
            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ -> {
                var alias = selectItemAlias(item, OutputAlias.expression(position));
                var qualifiedExpr = AttributeResolver.qualifyExpression(irExpr, availableAttrs);
                yield AttributeRef.expr(qualifiedExpr, alias);
            }
            case IRExpression.Aggregate agg -> {
                var alias = selectItemAlias(item, OutputAlias.aggregate(agg));
                yield AttributeRef.attr(alias, alias);
            }
            case IRExpression.Literal _, IRExpression.ScalarSubquery _ -> {
                var alias = selectItemAlias(item, OutputAlias.expression(position));
                var qualifiedExpr = AttributeResolver.qualifyExpression(irExpr, availableAttrs);
                yield AttributeRef.expr(qualifiedExpr, alias);
            }
        };
    }

    private String selectItemAlias(SelectItem<?> item, OutputAlias fallbackAlias) {
        return Option.ofNullable(item.getAlias())
            .map(Alias::getName)
            .orElseGet(fallbackAlias::toString);
    }

    private IRPipeline buildReturnForGroupBy(PlainSelect select, IRPipeline pipeline) {
        var selectItems = select.getSelectItems();
        if (isSelectAll(selectItems)) {
            return pipeline.returnAll();
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectedAttrs = IntStream.range(0, selectItems.size())
            .mapToObj(i -> buildGroupByAttributeRef(selectItems.get(i), availableAttrs, i + 1))
            .flatMap(Option::stream)
            .toList();

        return pipeline.returnSelected(selectedAttrs);
    }

    private Option<AttributeRef> buildGroupByAttributeRef(
        SelectItem<?> selectItem,
        List<String> availableAttrs,
        int position
    ) {
        var expr = selectItem.getExpression();
        if (expr instanceof AllColumns) {
            return Option.none();
        }

        if (expr instanceof Function fn && isAggregate(fn)) {
            var alias = selectItemAlias(selectItem, OutputAlias.aggregate(toAggregate(fn, null)));
            return Option.some(AttributeRef.attr(alias, alias));
        }

        var irExpr = toExpression(expr);
        return switch (irExpr) {
            case IRExpression.ColumnRef(var colName) -> {
                var resolvedName = AttributeResolver.resolve(colName, availableAttrs);
                var alias = selectItemAlias(selectItem, OutputAlias.column(colName));
                yield Option.some(AttributeRef.attr(resolvedName, alias));
            }
            case IRExpression.BinaryOp _, IRExpression.Cast _, IRExpression.CaseWhen _,
                 IRExpression.FunctionCall _ -> {
                var alias = selectItemAlias(selectItem, OutputAlias.expression(position));
                var qualifiedExpr = AttributeResolver.qualifyExpression(irExpr, availableAttrs);
                yield Option.some(AttributeRef.expr(qualifiedExpr, alias));
            }
            default -> throw new UnsupportedOperationException(
                "Unsupported expression in GROUP BY SELECT: " + irExpr.getClass().getSimpleName()
            );
        };
    }

    private IRPipeline addGroupBy(PlainSelect select, IRPipeline pipeline, List<String> extraGroupingAttributes) {
        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        var selectAliasSourceAttributes = new LinkedHashMap<String, String>();
        var selectAliasUnsupportedExpressionTypes = new LinkedHashMap<String, String>();
        buildGroupByAliasBindings(
            select.getSelectItems(),
            availableAttrs,
            selectAliasSourceAttributes,
            selectAliasUnsupportedExpressionTypes
        );

        List<String> groupingAttributes;
        if (select.getGroupBy() != null) {
            groupingAttributes = resolveGroupByAttributes(
                select,
                availableAttrs,
                selectAliasSourceAttributes,
                selectAliasUnsupportedExpressionTypes
            );
        } else {
            groupingAttributes = new ArrayList<>();
        }

        for (var attr : extraGroupingAttributes) {
            if (!groupingAttributes.contains(attr)) {
                groupingAttributes.add(attr);
            }
        }

        var selectItems = select.getSelectItems();
        var aggregates = new ArrayList<IRExpression.Aggregate>();

        for (var selectItem : selectItems) {
            var expr = selectItem.getExpression();
            if (expr instanceof AllColumns) continue;

            if (expr instanceof Function fn && isAggregate(fn)) {
                var baseAggregate = toAggregate(fn, null);
                var argument = baseAggregate.argument();
                var alias = selectItemAlias(
                    selectItem,
                    OutputAlias.aggregate(baseAggregate)
                );

                var qualifiedArgument = qualifyAggregateArgument(argument, availableAttrs);
                aggregates.add(new IRExpression.Aggregate(
                    baseAggregate.function(),
                    qualifiedArgument,
                    alias,
                    baseAggregate.distinct()
                ));
            }
        }

        var outputAttributes = new ArrayList<String>();
        outputAttributes.addAll(groupingAttributes);
        outputAttributes.addAll(aggregates.stream()
            .map(IRExpression.Aggregate::alias)
            .toList());

        return pipeline.group(groupingAttributes, aggregates, outputAttributes);
    }

    private void buildGroupByAliasBindings(
        List<SelectItem<?>> selectItems,
        List<String> availableAttrs,
        Map<String, String> aliasToSourceAttributes,
        Map<String, String> aliasToUnsupportedExpressionTypes
    ) {
        for (var selectItem : selectItems) {
            var expression = selectItem.getExpression();
            if (expression instanceof AllColumns) {
                continue;
            }

            Option.ofNullable(selectItem.getAlias())
                .map(Alias::getName)
                .stream()
                .forEach(aliasName -> bindGroupByAlias(
                    aliasName,
                    expression,
                    availableAttrs,
                    aliasToSourceAttributes,
                    aliasToUnsupportedExpressionTypes
                ));
        }
    }

    private void bindGroupByAlias(
        String aliasName,
        Expression expression,
        List<String> availableAttrs,
        Map<String, String> aliasToSourceAttributes,
        Map<String, String> aliasToUnsupportedExpressionTypes
    ) {
        var irExpression = toExpression(expression);
        switch (irExpression) {
            case IRExpression.ColumnRef(var columnName) ->
                aliasToSourceAttributes.put(
                    aliasName,
                    AttributeResolver.resolve(columnName, availableAttrs)
                );
            default ->
                aliasToUnsupportedExpressionTypes.put(
                    aliasName,
                    irExpression.getClass().getSimpleName()
                );
        }
    }

    private List<String> resolveGroupByAttributes(
        PlainSelect select,
        List<String> availableAttrs,
        Map<String, String> selectAliasSourceAttributes,
        Map<String, String> selectAliasUnsupportedExpressionTypes
    ) {
        var groupByAttributes = new ArrayList<String>();
        for (var expression : select.getGroupBy().getGroupByExpressionList()) {
            if (!(expression instanceof Expression groupByExpression)) {
                throw new UnsupportedOperationException(
                    "Unsupported GROUP BY item: " + expression
                );
            }
            groupByAttributes.add(resolveGroupByExpression(
                groupByExpression,
                availableAttrs,
                selectAliasSourceAttributes,
                selectAliasUnsupportedExpressionTypes
            ));
        }
        return groupByAttributes;
    }

    private String resolveGroupByExpression(
        Expression expression,
        List<String> availableAttrs,
        Map<String, String> selectAliasSourceAttributes,
        Map<String, String> selectAliasUnsupportedExpressionTypes
    ) {
        var irExpression = toExpression(expression);
        return switch (irExpression) {
            case IRExpression.ColumnRef(var columnRef) -> resolveGroupByAttribute(
                columnRef,
                availableAttrs,
                selectAliasSourceAttributes,
                selectAliasUnsupportedExpressionTypes
            );
            default -> throw new UnsupportedOperationException(
                (
                    "GROUP BY expression '%s' resolves to unsupported %s expression; " +
                        "only simple column references and simple column aliases are supported"
                ).formatted(expression, irExpression.getClass().getSimpleName())
            );
        };
    }

    private String resolveGroupByAttribute(
        String columnRef,
        List<String> availableAttrs,
        Map<String, String> selectAliasSourceAttributes,
        Map<String, String> selectAliasUnsupportedExpressionTypes
    ) {
        try {
            return AttributeResolver.resolve(columnRef, availableAttrs);
        } catch (IllegalArgumentException error) {
            return switch (Option.ofNullable(selectAliasSourceAttributes.get(columnRef))) {
                case Option.Some(var sourceAttribute) -> sourceAttribute;
                case Option.None() -> {
                    var expressionType = Option.ofNullable(
                        selectAliasUnsupportedExpressionTypes.get(columnRef)
                    ).orElseThrow(() -> error);
                    throw new UnsupportedOperationException(
                        (
                            "GROUP BY alias '%s' resolves to unsupported %s expression; " +
                                "only simple column aliases are supported"
                        ).formatted(columnRef, expressionType)
                    );
                }
            };
        }
    }

    private boolean isSelectAll(List<SelectItem<?>> selectItems) {
        return selectItems.size() == 1 && selectItems.getFirst().getExpression() instanceof AllColumns;
    }

    private List<Sort.SortKey> parseSortKeys(PlainSelect select, IRPipeline pipeline) {
        var orderByElements = select.getOrderByElements();
        if (orderByElements == null || orderByElements.isEmpty()) {
            return List.of();
        }

        if (isSelectStarOutput(pipeline.build())) {
            throw new UnsupportedOperationException("ORDER BY is not supported with SELECT *");
        }

        var availableAttrs = AttributeResolver.collectFrom(pipeline.build());
        return orderByElements.stream()
            .map(orderBy -> toSortKey(orderBy, availableAttrs))
            .toList();
    }

    private Sort.SortKey toSortKey(OrderByElement orderBy, List<String> availableAttrs) {
        if (orderBy.getNullOrdering() != null) {
            throw new UnsupportedOperationException("ORDER BY NULLS FIRST/LAST is not supported");
        }

        var sortAttribute = switch (orderBy.getExpression()) {
            case Column col -> AttributeResolver.resolve(toColumnRef(col).columnName(), availableAttrs);
            case LongValue ordinal -> resolveSortOrdinal(ordinal, availableAttrs);
            default -> throw new UnsupportedOperationException(
                "ORDER BY supports column references, aliases, and ordinals only");
        };

        var descending = orderBy.isAscDescPresent() && !orderBy.isAsc();
        return new Sort.SortKey(sortAttribute, descending);
    }

    private String resolveSortOrdinal(LongValue ordinalExpr, List<String> availableAttrs) {
        var ordinal = Math.toIntExact(ordinalExpr.getValue());
        if (ordinal < 1 || ordinal > availableAttrs.size()) {
            throw new IllegalArgumentException(
                "ORDER BY position %d is out of range for %d select items"
                    .formatted(ordinal, availableAttrs.size()));
        }
        return availableAttrs.get(ordinal - 1);
    }

    private Integer parseLimit(PlainSelect select) {
        var limit = select.getLimit();
        if (limit == null) {
            return null;
        }

        if (limit.getOffset() != null) {
            throw new UnsupportedOperationException("LIMIT with OFFSET is not supported");
        }

        var rowCount = limit.getRowCount();
        if (rowCount == null) {
            throw new UnsupportedOperationException("LIMIT requires an integer row count");
        }

        return switch (rowCount) {
            case LongValue lv -> {
                var value = Math.toIntExact(lv.getValue());
                if (value < 0) {
                    throw new IllegalArgumentException("LIMIT must be non-negative");
                }
                yield value;
            }
            default -> throw new UnsupportedOperationException(
                "LIMIT supports integer literals only");
        };
    }

    private boolean isSelectStarOutput(IRNode node) {
        return switch (node) {
            case Return ret -> ret.selectStar();
            case DuplElim de -> isSelectStarOutput(de.input());
            case Sort sort -> isSelectStarOutput(sort.input());
            default -> false;
        };
    }

    private IRExpression qualifyAggregateArgument(
        IRExpression expr, List<String> availableAttrs
    ) {
        return switch (expr) {
            case IRExpression.ColumnRef(var columnName) ->
                new IRExpression.ColumnRef(AttributeResolver.resolve(columnName, availableAttrs));
            case IRExpression.Literal lit -> lit;
            case IRExpression.Aggregate(var function, var argument, var alias, var distinct) ->
                new IRExpression.Aggregate(
                    function,
                    qualifyAggregateArgument(argument, availableAttrs),
                    alias,
                    distinct
                );
            case IRExpression.ScalarSubquery _ -> expr;
            case IRExpression.BinaryOp(var left, var op, var right) ->
                new IRExpression.BinaryOp(
                    qualifyAggregateArgument(left, availableAttrs),
                    op,
                    qualifyAggregateArgument(right, availableAttrs));
            case IRExpression.Cast(var inner, var targetType) ->
                new IRExpression.Cast(
                    qualifyAggregateArgument(inner, availableAttrs),
                    targetType
                );
            case IRExpression.FunctionCall(var name, var arguments) ->
                new IRExpression.FunctionCall(
                    name,
                    arguments.stream()
                        .map(arg -> qualifyAggregateArgument(arg, availableAttrs))
                        .toList()
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) -> {
                var qualifiedWhens = whens.stream()
                    .map(when -> new IRExpression.WhenClause(
                        AttributeResolver.qualifyCondition(when.condition(), availableAttrs),
                        qualifyAggregateArgument(when.result(), availableAttrs)
                    ))
                    .toList();
                var qualifiedElse = qualifyAggregateArgument(elseExpr, availableAttrs);
                yield new IRExpression.CaseWhen(qualifiedWhens, qualifiedElse);
            }
        };
    }

    private Option<IRExpression> qualifyAggregateArgument(
        Option<IRExpression> expr, List<String> availableAttrs
    ) {
        return expr.map(value -> qualifyAggregateArgument(value, availableAttrs));
    }

    private Option<AttributeRef> buildGroupedAttributeRef(
        SelectItem<?> item,
        List<String> availableAttrs,
        int position,
        AggregateCollector collector
    ) {
        if (item.getExpression() instanceof AllColumns) {
            return Option.none();
        }

        var rawExpr = toExpression(item.getExpression());
        if (!containsAggregate(rawExpr)) {
            return Option.some(buildGroupByAttributeRef(item, availableAttrs, position).orElseThrow(() ->
                new IllegalStateException("Expected grouped attribute reference")
            ));
        }

        var qualifiedExpr = qualifyAggregateArgument(rawExpr, availableAttrs);

        if (qualifiedExpr instanceof IRExpression.Aggregate aggregate) {
            var alias = selectItemAlias(item, OutputAlias.aggregate(aggregate));
            collector.add(new IRExpression.Aggregate(
                aggregate.function(),
                aggregate.argument(),
                alias,
                aggregate.distinct()
            ));
            return Option.some(AttributeRef.attr(alias, alias));
        }

        var rewrittenExpr = rewriteAggregateExpression(qualifiedExpr, collector);
        var alias = selectItemAlias(item, OutputAlias.expression(position));
        return Option.some(AttributeRef.expr(rewrittenExpr, alias));
    }

    private boolean containsAggregate(IRExpression expr) {
        return switch (expr) {
            case IRExpression.Aggregate _ -> true;
            case IRExpression.BinaryOp(var left, _, var right) ->
                containsAggregate(left) || containsAggregate(right);
            case IRExpression.Cast(var inner, _) -> containsAggregate(inner);
            case IRExpression.FunctionCall(_, var arguments) ->
                arguments.stream().anyMatch(this::containsAggregate);
            case IRExpression.CaseWhen(var whens, var elseExpr) ->
                whens.stream().anyMatch(when ->
                    containsAggregateInCondition(when.condition()) || containsAggregate(when.result()))
                    || elseExpr.stream().anyMatch(this::containsAggregate);
            case IRExpression.ColumnRef _, IRExpression.Literal _, IRExpression.ScalarSubquery _ -> false;
        };
    }

    private boolean containsAggregateInCondition(Condition condition) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, _) ->
                containsAggregate(left) || containsAggregate(right);
            case Condition.IsNull _ -> false;
            case Condition.Like(var left, var pattern, _) ->
                containsAggregate(left) || containsAggregate(pattern);
            case Condition.Exists _, Condition.InSubquery _ -> false;
            case Condition.And(var operands) ->
                operands.stream().anyMatch(this::containsAggregateInCondition);
            case Condition.Or(var operands) ->
                operands.stream().anyMatch(this::containsAggregateInCondition);
            case Condition.Not(var operand) -> containsAggregateInCondition(operand);
        };
    }

    private IRExpression rewriteAggregateExpression(IRExpression expr, AggregateCollector collector) {
        return switch (expr) {
            case IRExpression.Aggregate aggregate ->
                collector.reference((IRExpression.Aggregate) qualifyAggregateArgument(
                    aggregate,
                    collector.availableAttrs()
                ));
            case IRExpression.BinaryOp(var left, var op, var right) ->
                new IRExpression.BinaryOp(
                    rewriteAggregateExpression(left, collector),
                    op,
                    rewriteAggregateExpression(right, collector)
                );
            case IRExpression.Cast(var inner, var targetType) ->
                new IRExpression.Cast(rewriteAggregateExpression(inner, collector), targetType);
            case IRExpression.FunctionCall(var name, var arguments) ->
                new IRExpression.FunctionCall(
                    name,
                    arguments.stream()
                        .map(argument -> rewriteAggregateExpression(argument, collector))
                        .toList()
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) -> new IRExpression.CaseWhen(
                whens.stream()
                    .map(when -> new IRExpression.WhenClause(
                        rewriteAggregateCondition(when.condition(), collector),
                        rewriteAggregateExpression(when.result(), collector)
                    ))
                    .toList(),
                elseExpr.map(exprValue -> rewriteAggregateExpression(exprValue, collector))
            );
            case IRExpression.ColumnRef _, IRExpression.Literal _, IRExpression.ScalarSubquery _ -> expr;
        };
    }

    private Condition rewriteAggregateCondition(Condition condition, AggregateCollector collector) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var op) ->
                Condition.compare(
                    rewriteAggregateExpression(left, collector),
                    op,
                    rewriteAggregateExpression(right, collector)
                );
            case Condition.IsNull isNull -> isNull;
            case Condition.Like(var left, var pattern, var negated) ->
                new Condition.Like(
                    rewriteAggregateExpression(left, collector),
                    rewriteAggregateExpression(pattern, collector),
                    negated
                );
            case Condition.Exists exists -> exists;
            case Condition.InSubquery(var left, var subquery, var negated) ->
                new Condition.InSubquery(
                    rewriteAggregateExpression(left, collector),
                    subquery,
                    negated
                );
            case Condition.And(var operands) ->
                Condition.and(operands.stream()
                    .map(operand -> rewriteAggregateCondition(operand, collector))
                    .toList());
            case Condition.Or(var operands) ->
                Condition.or(operands.stream()
                    .map(operand -> rewriteAggregateCondition(operand, collector))
                    .toList());
            case Condition.Not(var operand) ->
                Condition.not(rewriteAggregateCondition(operand, collector));
        };
    }

    private Condition qualifyGroupedCondition(Condition condition, List<String> availableAttrs) {
        return switch (condition) {
            case Condition.Comparison(var left, var right, var op) ->
                Condition.compare(
                    qualifyGroupedExpression(left, availableAttrs),
                    op,
                    qualifyGroupedExpression(right, availableAttrs)
                );
            case Condition.IsNull(var attr, var negated) ->
                new Condition.IsNull(AttributeResolver.resolve(attr, availableAttrs), negated);
            case Condition.Like(var left, var pattern, var negated) ->
                new Condition.Like(
                    qualifyGroupedExpression(left, availableAttrs),
                    qualifyGroupedExpression(pattern, availableAttrs),
                    negated
                );
            case Condition.Exists exists -> exists;
            case Condition.InSubquery(var left, var subquery, var negated) ->
                new Condition.InSubquery(
                    qualifyGroupedExpression(left, availableAttrs),
                    subquery,
                    negated
                );
            case Condition.And(var operands) ->
                Condition.and(operands.stream()
                    .map(operand -> qualifyGroupedCondition(operand, availableAttrs))
                    .toList());
            case Condition.Or(var operands) ->
                Condition.or(operands.stream()
                    .map(operand -> qualifyGroupedCondition(operand, availableAttrs))
                    .toList());
            case Condition.Not(var operand) ->
                Condition.not(qualifyGroupedCondition(operand, availableAttrs));
        };
    }

    private IRExpression qualifyGroupedExpression(IRExpression expr, List<String> availableAttrs) {
        return switch (expr) {
            case IRExpression.ColumnRef(var columnName) ->
                new IRExpression.ColumnRef(AttributeResolver.resolve(columnName, availableAttrs));
            case IRExpression.BinaryOp(var left, var op, var right) ->
                new IRExpression.BinaryOp(
                    qualifyGroupedExpression(left, availableAttrs),
                    op,
                    qualifyGroupedExpression(right, availableAttrs)
                );
            case IRExpression.Cast(var inner, var targetType) ->
                new IRExpression.Cast(qualifyGroupedExpression(inner, availableAttrs), targetType);
            case IRExpression.FunctionCall(var name, var arguments) ->
                new IRExpression.FunctionCall(
                    name,
                    arguments.stream()
                        .map(argument -> qualifyGroupedExpression(argument, availableAttrs))
                        .toList()
                );
            case IRExpression.CaseWhen(var whens, var elseExpr) -> new IRExpression.CaseWhen(
                whens.stream()
                    .map(when -> new IRExpression.WhenClause(
                        qualifyGroupedCondition(when.condition(), availableAttrs),
                        qualifyGroupedExpression(when.result(), availableAttrs)
                    ))
                    .toList(),
                elseExpr.map(exprValue -> qualifyGroupedExpression(exprValue, availableAttrs))
            );
            case IRExpression.Literal _, IRExpression.Aggregate _, IRExpression.ScalarSubquery _ -> expr;
        };
    }

    private record GroupedQueryPreparation(
        List<String> groupingAttributes,
        List<IRExpression.Aggregate> aggregates,
        List<String> outputAttributes,
        Option<Condition> havingCondition,
        List<AttributeRef> selectedAttributes,
        boolean selectStar
    ) {
    }

    private static final class AggregateCollector {
        private final List<IRExpression.Aggregate> aggregates = new ArrayList<>();
        private final List<String> availableAttrs;
        private int hiddenAggregateCounter = 0;

        private AggregateCollector(List<String> availableAttrs) {
            this.availableAttrs = availableAttrs;
        }

        private IRExpression.ColumnRef reference(IRExpression.Aggregate aggregate) {
            var alias = nextHiddenAlias();
            add(new IRExpression.Aggregate(
                aggregate.function(),
                aggregate.argument(),
                alias,
                aggregate.distinct()
            ));
            return new IRExpression.ColumnRef(alias);
        }

        private void add(IRExpression.Aggregate aggregate) {
            if (availableAttrs.contains(aggregate.alias())
                || aggregates.stream().anyMatch(existing -> existing.alias().equals(aggregate.alias()))) {
                throw new IllegalArgumentException(
                    "Aggregate alias '%s' conflicts with existing attributes".formatted(aggregate.alias())
                );
            }
            aggregates.add(aggregate);
        }

        private String nextHiddenAlias() {
            hiddenAggregateCounter += 1;
            return "agg_expr_" + hiddenAggregateCounter;
        }

        private List<IRExpression.Aggregate> aggregates() {
            return List.copyOf(aggregates);
        }

        private List<String> availableAttrs() {
            return availableAttrs;
        }
    }
}
