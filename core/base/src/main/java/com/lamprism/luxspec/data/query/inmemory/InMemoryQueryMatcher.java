/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.data.query.inmemory;

import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.ExistsCondition;
import com.lamprism.luxspec.data.query.InMemoryQueryAggregate;
import com.lamprism.luxspec.data.query.InMemoryQueryFunction;
import com.lamprism.luxspec.data.query.InMemoryQueryOperator;
import com.lamprism.luxspec.data.query.LikeCondition;
import com.lamprism.luxspec.data.query.LogicalCondition;
import com.lamprism.luxspec.data.query.LogicalOperator;
import com.lamprism.luxspec.data.query.NegatedQueryExpression;
import com.lamprism.luxspec.data.query.QueryAggregateDefinition;
import com.lamprism.luxspec.data.query.QueryAggregateFunction;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryExpressionVisitor;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryFieldResolver;
import com.lamprism.luxspec.data.query.QueryFunctionDefinition;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QueryOperatorDefinition;
import com.lamprism.luxspec.data.query.QueryRelation;
import com.lamprism.luxspec.data.query.QueryRelationResolver;
import com.lamprism.luxspec.data.query.QueryValue;
import com.lamprism.luxspec.data.query.QueryValueCondition;
import com.lamprism.luxspec.data.query.QueryValueVisitor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates query expressions and values against one in-memory candidate scope.
 *
 * <p>This class owns expression semantics only. Candidate collection traversal, ordering, and
 * result windows belong to {@link InMemoryQueryEngine}.</p>
 *
 * @param <T> the candidate type
 *
 * @author RollW
 */
final class InMemoryQueryMatcher<T>
        implements QueryExpressionVisitor<Boolean>, QueryValueVisitor<Object> {
    private final QueryFieldResolver<T> fieldResolver;
    private final QueryRelationResolver<T> relationResolver;
    private final Map<QueryRelation<?>, QueryFieldResolver<?>> relatedFieldResolvers;
    private final Map<QueryRelation<?>, QueryRelationResolver<?>> relatedRelationResolvers;
    private final Map<QueryFunctionDefinition<?>, InMemoryQueryFunction<?>> functions;
    private final Map<QueryOperatorDefinition<?, ?>, InMemoryQueryOperator<?, ?>> operators;
    private final Map<QueryAggregateDefinition<?>, InMemoryQueryAggregate<?>> aggregates;
    private final @Nullable T candidate;
    private final @Nullable List<? extends T> aggregationCandidates;

    /**
     * Creates a matcher for one candidate.
     *
     * @param fieldResolver            the current-scope field resolver
     * @param relationResolver         the current-scope relationship resolver
     * @param relatedFieldResolvers    field resolvers keyed by relationship
     * @param relatedRelationResolvers relationship resolvers keyed by nested relationship
     * @param candidate                the candidate to evaluate
     */
    InMemoryQueryMatcher(
            QueryFieldResolver<T> fieldResolver,
            QueryRelationResolver<T> relationResolver,
            Map<QueryRelation<?>, QueryFieldResolver<?>> relatedFieldResolvers,
            Map<QueryRelation<?>, QueryRelationResolver<?>> relatedRelationResolvers,
            Map<QueryFunctionDefinition<?>, InMemoryQueryFunction<?>> functions,
            Map<QueryOperatorDefinition<?, ?>, InMemoryQueryOperator<?, ?>> operators,
            Map<QueryAggregateDefinition<?>, InMemoryQueryAggregate<?>> aggregates,
            @Nullable T candidate
    ) {
        this(
                fieldResolver,
                relationResolver,
                relatedFieldResolvers,
                relatedRelationResolvers,
                functions,
                operators,
                aggregates,
                candidate,
                null
        );
    }

    /**
     * Creates a matcher for one candidate and an aggregate group.
     *
     * @param fieldResolver            the current-scope field resolver
     * @param relationResolver         the current-scope relationship resolver
     * @param relatedFieldResolvers    field resolvers keyed by relationship
     * @param relatedRelationResolvers relationship resolvers keyed by nested relationship
     * @param candidate                the candidate to evaluate
     * @param aggregationCandidates    the current aggregate group, when applicable
     */
    InMemoryQueryMatcher(
            QueryFieldResolver<T> fieldResolver,
            QueryRelationResolver<T> relationResolver,
            Map<QueryRelation<?>, QueryFieldResolver<?>> relatedFieldResolvers,
            Map<QueryRelation<?>, QueryRelationResolver<?>> relatedRelationResolvers,
            Map<QueryFunctionDefinition<?>, InMemoryQueryFunction<?>> functions,
            Map<QueryOperatorDefinition<?, ?>, InMemoryQueryOperator<?, ?>> operators,
            Map<QueryAggregateDefinition<?>, InMemoryQueryAggregate<?>> aggregates,
            @Nullable T candidate,
            @Nullable List<? extends T> aggregationCandidates
    ) {
        this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
        this.relationResolver = Objects.requireNonNull(relationResolver, "relationResolver");
        this.relatedFieldResolvers = Objects.requireNonNull(relatedFieldResolvers, "relatedFieldResolvers");
        this.relatedRelationResolvers = Objects.requireNonNull(
                relatedRelationResolvers,
                "relatedRelationResolvers"
        );
        this.functions = Objects.requireNonNull(functions, "functions");
        this.operators = Objects.requireNonNull(operators, "operators");
        this.aggregates = Objects.requireNonNull(aggregates, "aggregates");
        this.candidate = candidate;
        this.aggregationCandidates = aggregationCandidates;
    }

    /**
     * Evaluates a boolean query expression.
     *
     * @param expression the expression to evaluate
     * @return whether the candidate matches
     */
    boolean matches(QueryExpression expression) {
        return Objects.requireNonNull(expression, "expression").accept(this);
    }

    @Override
    public Boolean visitMatchAll() {
        return true;
    }

    @Override
    public <V, A> Boolean visitCondition(QueryCondition<V, A> condition) {
        V candidateValue = fieldResolver.resolve(candidate(), condition.getField());
        return matchesValue(condition.getOperator(), candidateValue, condition.getValues());
    }

    @Override
    public <V, A> Boolean visitValueCondition(QueryValueCondition<V, A> condition) {
        Object candidateValue = condition.getValue().accept(this);
        return matchesValue(condition.getOperator(), candidateValue, condition.getValues());
    }

    @Override
    public <V extends Comparable<? super V>> Boolean visitComparison(ComparisonCondition<V> condition) {
        V candidateValue = fieldResolver.resolve(candidate(), condition.getField());
        return matchesValue(condition.getOperator(), candidateValue, List.of(condition.getValue()));
    }

    @Override
    public Boolean visitLike(LikeCondition condition) {
        String candidateValue = fieldResolver.resolve(candidate(), condition.getField());
        return candidateValue != null && matchesLike(candidateValue, condition.getPattern());
    }

    @Override
    public Boolean visitLogical(LogicalCondition condition) {
        if (condition.getOperator() == LogicalOperator.AND) {
            return matchesAll(condition.getExpressions());
        }
        return matchesAny(condition.getExpressions());
    }

    @Override
    public Boolean visitNegated(NegatedQueryExpression condition) {
        return !matches(condition.getExpression());
    }

    @Override
    public <R> Boolean visitExists(ExistsCondition<R> condition) {
        Iterable<?> related = relationResolver.resolve(candidate(), condition.getRelation());
        if (related == null) {
            return false;
        }
        QueryFieldResolver<R> relatedResolver = relatedFieldResolver(condition.getRelation());
        return matchesRelated(related, relatedResolver, condition.getCondition());
    }

    @SuppressWarnings("unchecked")
    private <R> QueryFieldResolver<R> relatedFieldResolver(QueryRelation<R> relation) {
        QueryFieldResolver<?> resolver = relatedFieldResolvers.get(relation);
        if (resolver != null) {
            return (QueryFieldResolver<R>) resolver;
        }
        throw new IllegalArgumentException(
                "Query relation field resolver is not configured: " + relation.getName()
        );
    }

    private <R> boolean matchesRelated(
            Iterable<?> related,
            QueryFieldResolver<R> relatedResolver,
            QueryExpression condition
    ) {
        for (Object relatedValue : related) {
            if (relatedValue == null) {
                continue;
            }
            if (matchesRelatedCandidate(relatedValue, relatedResolver, condition)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <R> boolean matchesRelatedCandidate(
            Object relatedValue,
            QueryFieldResolver<R> relatedResolver,
            QueryExpression condition
    ) {
        R relatedCandidate = (R) relatedValue;
        return new InMemoryQueryMatcher<>(
                relatedResolver,
                nestedRelationResolver(relatedRelationResolvers),
                relatedFieldResolvers,
                relatedRelationResolvers,
                functions,
                operators,
                aggregates,
                relatedCandidate
        ).matches(condition);
    }

    @Override
    public <V> Object visitField(QueryField<V> field) {
        return fieldResolver.resolve(candidate(), field);
    }

    @Override
    public <V> Object visitLiteral(QueryValue.Literal<V> value) {
        return value.getValue();
    }

    @Override
    public <V> Object visitFunction(QueryValue.Function<V> value) {
        return switch (value.getFunction()) {
            case LOWER -> transformString(value.getArguments().get(0), false);
            case UPPER -> transformString(value.getArguments().get(0), true);
            case COALESCE -> {
                Object first = evaluate(value.getArguments().get(0));
                yield first != null ? first : evaluate(value.getArguments().get(1));
            }
            case CAST -> throw new IllegalArgumentException("CAST is represented by a cast value");
        };
    }

    @Override
    public <V> Object visitCustomFunction(QueryValue.CustomFunction<V> value) {
        InMemoryQueryFunction<?> function = functions.get(value.getDefinition());
        if (function == null) {
            throw new IllegalArgumentException("In-memory query function is not registered: "
                    + value.getDefinition().getName());
        }
        List<@Nullable Object> arguments = new ArrayList<>(value.getArguments().size());
        for (QueryValue<?> argument : value.getArguments()) {
            arguments.add(evaluate(argument));
        }
        @Nullable Object result = function.evaluate(Collections.unmodifiableList(arguments));
        if (result != null && !value.getValueType().isInstance(result)) {
            throw new IllegalStateException("In-memory query function returned an incompatible value");
        }
        return result;
    }

    @Override
    public <V> Object visitCast(QueryValue.Cast<V> value) {
        Object operand = evaluate(value.getOperand());
        if (operand == null) {
            return null;
        }
        if (value.getTargetType().isInstance(operand)) {
            return operand;
        }
        if (value.getTargetType() == String.class) {
            return String.valueOf(operand);
        }
        throw new IllegalArgumentException(
                "In-memory cast does not support " + operand.getClass().getName()
                        + " to " + value.getTargetType().getName()
        );
    }

    @Override
    public <V> Object visitAggregate(QueryValue.Aggregate<V> value) {
        List<? extends T> group = aggregationCandidates;
        if (group == null) {
            throw new IllegalArgumentException("Aggregate values require a query plan");
        }
        if (value.getFunction() instanceof QueryAggregateDefinition<?> definition) {
            return customAggregate(definition, value.getArguments(), group);
        }
        if (!(value.getFunction() instanceof QueryAggregateFunction.Standard standard)) {
            throw new IllegalArgumentException("Unsupported query aggregate");
        }
        if (standard == QueryAggregateFunction.COUNT && value.getArguments().isEmpty()) {
            return (long) group.size();
        }
        QueryValue<?> operand = value.getArguments().get(0);
        List<Object> values = new ArrayList<>(group.size());
        for (T groupCandidate : group) {
            Object resolved = new InMemoryQueryMatcher<>(
                    fieldResolver,
                    relationResolver,
                    relatedFieldResolvers,
                    relatedRelationResolvers,
                    functions,
                    operators,
                    aggregates,
                    groupCandidate,
                    group
            ).evaluate(operand);
            if (resolved != null) {
                values.add(resolved);
            }
        }
        return switch (standard) {
            case COUNT, COUNT_DISTINCT -> (long) distinctValues(values, standard ==
                    QueryAggregateFunction.COUNT_DISTINCT).size();
            case SUM -> sum(values);
            case AVERAGE -> average(values);
            case MIN -> minimum(values);
            case MAX -> maximum(values);
        };
    }

    private @Nullable Object customAggregate(
            QueryAggregateDefinition<?> definition, List<QueryValue<?>> arguments, List<? extends T> group
    ) {
        InMemoryQueryAggregate<?> implementation = aggregates.get(definition);
        if (implementation == null) {
            throw new IllegalArgumentException("In-memory query aggregate is not registered: " + definition.getName());
        }
        List<List<@Nullable Object>> rows = new ArrayList<>(group.size());
        for (T groupCandidate : group) {
            InMemoryQueryMatcher<T> matcher = new InMemoryQueryMatcher<>(fieldResolver, relationResolver,
                    relatedFieldResolvers, relatedRelationResolvers, functions, operators, aggregates, groupCandidate);
            List<@Nullable Object> row = new ArrayList<>(arguments.size());
            for (QueryValue<?> argument : arguments) {
                row.add(matcher.evaluate(argument));
            }
            rows.add(Collections.unmodifiableList(row));
        }
        @Nullable Object result = implementation.evaluate(List.copyOf(rows));
        if (result != null && !definition.getResultType().isInstance(result)) {
            throw new IllegalStateException("In-memory query aggregate returned an incompatible value");
        }
        return result;
    }

    /**
     * Evaluates a typed query value.
     *
     * @param value the value to evaluate
     * @return the resolved value
     */
    Object evaluate(QueryValue<?> value) {
        return Objects.requireNonNull(value, "value").accept(this);
    }

    private Object transformString(QueryValue<?> value, boolean upper) {
        Object resolved = evaluate(value);
        if (resolved == null) {
            return null;
        }
        if (!(resolved instanceof String string)) {
            throw new IllegalArgumentException("String functions require a string operand");
        }
        return upper ? string.toUpperCase(Locale.ROOT) : string.toLowerCase(Locale.ROOT);
    }

    private T candidate() {
        return Objects.requireNonNull(candidate, "query candidate");
    }

    private static List<Object> distinctValues(List<Object> values, boolean distinct) {
        if (!distinct) {
            return values;
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private static BigDecimal sum(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (Object value : values) {
            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("SUM requires numeric values");
            }
            result = result.add(toBigDecimal(number));
        }
        return result;
    }

    private static Double average(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        }
        double result = 0.0d;
        for (Object value : values) {
            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("AVERAGE requires numeric values");
            }
            result += number.doubleValue();
        }
        return result / values.size();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object minimum(List<Object> values) {
        return extreme(values, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object maximum(List<Object> values) {
        return extreme(values, true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object extreme(List<Object> values, boolean maximum) {
        if (values.isEmpty()) {
            return null;
        }
        Object result = values.get(0);
        if (!(result instanceof Comparable comparable)) {
            throw new IllegalArgumentException("MIN and MAX require comparable values");
        }
        for (int index = 1; index < values.size(); index++) {
            Object candidateValue = values.get(index);
            if (!(candidateValue instanceof Comparable comparableValue)) {
                throw new IllegalArgumentException("MIN and MAX require comparable values");
            }
            int comparison = comparable.compareTo(candidateValue);
            if ((maximum && comparison < 0) || (!maximum && comparison > 0)) {
                result = candidateValue;
                comparable = comparableValue;
            }
        }
        return result;
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(number.toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compare(Object left, Object right) {
        if (left == null) {
            return -1;
        }
        if (!(left instanceof Comparable comparable)) {
            throw new IllegalArgumentException("Query value is not Comparable");
        }
        return comparable.compareTo(right);
    }

    private boolean matchesValue(
            QueryOperator operator,
            @Nullable Object candidateValue,
            List<?> values
    ) {
        if (operator instanceof QueryOperatorDefinition<?, ?> definition) {
            return matchCustom(definition, candidateValue, values);
        }
        if (!(operator instanceof QueryOperator.Standard standard)) {
            throw new IllegalArgumentException("Unsupported query operator");
        }
        if (candidateValue == null) {
            return operator == QueryOperator.IS_NULL;
        }
        return switch (standard) {
            case EQUAL -> Objects.equals(candidateValue, values.get(0));
            case NOT_EQUAL -> !Objects.equals(candidateValue, values.get(0));
            case IN -> values.contains(candidateValue);
            case NOT_IN -> !values.contains(candidateValue);
            case IS_NULL -> candidateValue == null;
            case IS_NOT_NULL -> candidateValue != null;
            case LIKE -> candidateValue instanceof String string
                    && matchesLike(string, (String) values.get(0));
            case GREATER_THAN -> compare(candidateValue, values.get(0)) > 0;
            case GREATER_THAN_OR_EQUAL -> compare(candidateValue, values.get(0)) >= 0;
            case LESS_THAN -> compare(candidateValue, values.get(0)) < 0;
            case LESS_THAN_OR_EQUAL -> compare(candidateValue, values.get(0)) <= 0;
        };
    }

    @SuppressWarnings("unchecked")
    private <V, A> boolean matchCustom(
            QueryOperatorDefinition<V, A> definition, @Nullable Object operand, List<?> values
    ) {
        InMemoryQueryOperator<?, ?> implementation = operators.get(definition);
        if (implementation == null) {
            throw new IllegalArgumentException("In-memory query operator is not registered: " + definition.getName());
        }
        List<A> arguments = values.stream().map(definition.getArgumentType()::cast).toList();
        return ((InMemoryQueryOperator<V, A>) implementation).test(definition.getOperandType().cast(operand), arguments);
    }

    private boolean matchesAll(List<QueryExpression> expressions) {
        for (QueryExpression expression : expressions) {
            if (!matches(expression)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAny(List<QueryExpression> expressions) {
        for (QueryExpression expression : expressions) {
            if (matches(expression)) {
                return true;
            }
        }
        return false;
    }

    private static <R> QueryRelationResolver<R> nestedRelationResolver(
            Map<QueryRelation<?>, QueryRelationResolver<?>> relationResolvers
    ) {
        return (candidate, relation) -> {
            QueryRelationResolver<?> resolver = relationResolvers.get(relation);
            if (resolver == null) {
                throw new IllegalArgumentException(
                        "Nested query relation resolver is not configured: " + relation.getName()
                );
            }
            return resolveNestedRelation(resolver, candidate, relation);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    private static Iterable<?> resolveNestedRelation(
            QueryRelationResolver<?> resolver,
            Object candidate,
            QueryRelation<?> relation
    ) {
        return ((QueryRelationResolver) resolver).resolve(candidate, relation);
    }

    private static boolean matchesLike(String value, String pattern) {
        int valueIndex = 0;
        int patternIndex = 0;
        int wildcardIndex = -1;
        int wildcardValueIndex = -1;
        while (valueIndex < value.length()) {
            if (patternIndex >= pattern.length()) {
                if (wildcardIndex < 0) {
                    return false;
                }
                wildcardValueIndex++;
                valueIndex = wildcardValueIndex;
                patternIndex = wildcardIndex + 1;
                continue;
            }
            char patternCharacter = pattern.charAt(patternIndex);
            if (patternCharacter == LikeCondition.ESCAPE_CHARACTER
                    && matchesEscapedCharacter(value, pattern, valueIndex, patternIndex)) {
                valueIndex++;
                patternIndex += 2;
                continue;
            }
            if (patternCharacter == '_') {
                valueIndex++;
                patternIndex++;
                continue;
            }
            if (patternCharacter == '%') {
                wildcardIndex = patternIndex;
                wildcardValueIndex = valueIndex;
                patternIndex++;
                continue;
            }
            if (patternCharacter != LikeCondition.ESCAPE_CHARACTER
                    && value.charAt(valueIndex) == patternCharacter) {
                valueIndex++;
                patternIndex++;
                continue;
            }
            if (wildcardIndex < 0) {
                return false;
            }
            wildcardValueIndex++;
            valueIndex = wildcardValueIndex;
            patternIndex = wildcardIndex + 1;
        }
        while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '%') {
            patternIndex++;
        }
        return patternIndex == pattern.length();
    }

    private static boolean matchesEscapedCharacter(
            String value,
            String pattern,
            int valueIndex,
            int patternIndex
    ) {
        return patternIndex + 1 < pattern.length()
                && value.charAt(valueIndex) == pattern.charAt(patternIndex + 1);
    }
}
