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

import com.lamprism.luxspec.data.pagination.CompleteResult;
import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.pagination.KeysetResult;
import com.lamprism.luxspec.data.pagination.KeysetWindow;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.TimeSeriesWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.ExistsCondition;
import com.lamprism.luxspec.data.query.InMemoryQueryAggregate;
import com.lamprism.luxspec.data.query.InMemoryQueryExecutor;
import com.lamprism.luxspec.data.query.InMemoryQueryFunction;
import com.lamprism.luxspec.data.query.InMemoryQueryOperator;
import com.lamprism.luxspec.data.query.LogicalCondition;
import com.lamprism.luxspec.data.query.NegatedQueryExpression;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.ProjectionQueryPlan;
import com.lamprism.luxspec.data.query.QueryAggregateDefinition;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryFieldResolver;
import com.lamprism.luxspec.data.query.QueryFunctionDefinition;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QueryOperatorDefinition;
import com.lamprism.luxspec.data.query.QueryPlan;
import com.lamprism.luxspec.data.query.QueryProjection;
import com.lamprism.luxspec.data.query.QueryRelation;
import com.lamprism.luxspec.data.query.QueryRelationResolver;
import com.lamprism.luxspec.data.query.QueryValue;
import com.lamprism.luxspec.data.query.QueryValueCondition;
import com.lamprism.luxspec.data.query.TimeSeriesAggregation;
import com.lamprism.luxspec.data.query.TimeSeriesQueryPlan;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Executes the provider-independent query model over an in-memory candidate collection.
 *
 * <p>The public construction surface remains {@link InMemoryQueryExecutor}; this class owns
 * collection traversal, ordering, grouping, and result windows.</p>
 *
 * @param <T> the queried candidate type
 *
 * @author RollW
 */
public final class InMemoryQueryEngine<T> {
    private final Supplier<? extends Collection<? extends T>> candidateSupplier;
    private final QueryFieldResolver<T> fieldResolver;
    private final QueryRelationResolver<T> relationResolver;
    private final Map<QueryRelation<?>, QueryFieldResolver<?>> relatedFieldResolvers;
    private final Map<QueryRelation<?>, QueryRelationResolver<?>> relatedRelationResolvers;
    private final Map<QueryFunctionDefinition<?>, InMemoryQueryFunction<?>> functions;
    private final Map<QueryOperatorDefinition<?, ?>, InMemoryQueryOperator<?, ?>> operators;
    private final Map<QueryAggregateDefinition<?>, InMemoryQueryAggregate<?>> aggregates;
    private final InMemoryQueryWindow<T> windowing;

    /**
     * Creates an in-memory query engine.
     *
     * @param candidateSupplier        the candidate source evaluated once per operation
     * @param fieldResolver            the root field resolver
     * @param relationResolver         the root relationship resolver
     * @param relatedFieldResolvers    field resolvers keyed by relationship
     * @param relatedRelationResolvers relationship resolvers keyed by nested relationship
     */
    public InMemoryQueryEngine(
            Supplier<? extends Collection<? extends T>> candidateSupplier,
            QueryFieldResolver<T> fieldResolver,
            QueryRelationResolver<T> relationResolver,
            Map<QueryRelation<?>, QueryFieldResolver<?>> relatedFieldResolvers,
            Map<QueryRelation<?>, QueryRelationResolver<?>> relatedRelationResolvers
    ) {
        this(candidateSupplier, fieldResolver, relationResolver, relatedFieldResolvers, relatedRelationResolvers,
                Map.of(), Map.of(), Map.of());
    }

    /**
     * Creates an engine with explicitly registered application functions and operators.
     *
     * @param candidateSupplier        the candidate source
     * @param fieldResolver            the field resolver
     * @param relationResolver         the root relationship resolver
     * @param relatedFieldResolvers    related field resolvers
     * @param relatedRelationResolvers nested relationship resolvers
     * @param functions                the trusted application function implementations
     * @param operators                the trusted application operator implementations
     * @param aggregates               the trusted application aggregate implementations
     */
    public InMemoryQueryEngine(
            Supplier<? extends Collection<? extends T>> candidateSupplier,
            QueryFieldResolver<T> fieldResolver,
            QueryRelationResolver<T> relationResolver,
            Map<QueryRelation<?>, QueryFieldResolver<?>> relatedFieldResolvers,
            Map<QueryRelation<?>, QueryRelationResolver<?>> relatedRelationResolvers,
            Map<QueryFunctionDefinition<?>, InMemoryQueryFunction<?>> functions,
            Map<QueryOperatorDefinition<?, ?>, InMemoryQueryOperator<?, ?>> operators,
            Map<QueryAggregateDefinition<?>, InMemoryQueryAggregate<?>> aggregates
    ) {
        this.candidateSupplier = Objects.requireNonNull(candidateSupplier, "candidateSupplier");
        this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
        this.relationResolver = Objects.requireNonNull(relationResolver, "relationResolver");
        this.relatedFieldResolvers = Map.copyOf(
                Objects.requireNonNull(relatedFieldResolvers, "relatedFieldResolvers")
        );
        this.relatedRelationResolvers = Map.copyOf(
                Objects.requireNonNull(relatedRelationResolvers, "relatedRelationResolvers")
        );
        this.functions = Map.copyOf(Objects.requireNonNull(functions, "functions"));
        this.operators = Map.copyOf(Objects.requireNonNull(operators, "operators"));
        this.aggregates = Map.copyOf(Objects.requireNonNull(aggregates, "aggregates"));
        this.windowing = new InMemoryQueryWindow<>(this.fieldResolver);
    }

    /**
     * Executes criteria over every candidate supplied for this operation.
     *
     * @param criteria the validated filters and ordering
     * @param window   the requested result window
     * @return the complete, page, or slice result
     */
    public QueryResult<T> query(QueryCriteria criteria, QueryWindow window) {
        return query(candidate -> true, criteria, window);
    }

    /**
     * Executes criteria over candidates accepted by an in-memory post-condition filter.
     *
     * @param candidateFilter the restriction applied before ordering and windowing
     * @param criteria        the validated filters and ordering
     * @param window          the requested result window
     * @return the complete, page, or slice result
     */
    public QueryResult<T> query(
            Predicate<? super T> candidateFilter,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        Predicate<? super T> nonNullCandidateFilter = Objects.requireNonNull(candidateFilter, "candidateFilter");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        windowing.requireSupported(nonNullWindow);
        requireCapabilities(nonNullCriteria);
        return query(
                candidates(),
                nonNullCandidateFilter,
                nonNullCriteria,
                nonNullWindow
        );
    }

    /**
     * Executes a projection, aggregate, or time-series plan.
     *
     * @param plan   the validated query plan
     * @param window the requested result window
     * @param <R>    the plan result type
     * @return the plan result
     */
    public <R> QueryResult<R> query(QueryPlan<R> plan, QueryWindow window) {
        QueryPlan<R> nonNullPlan = Objects.requireNonNull(plan, "plan");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        windowing.requireSupported(nonNullWindow);
        if (!(nonNullPlan instanceof ProjectionQueryPlan<?>) && !(nonNullPlan instanceof TimeSeriesQueryPlan<?>)) {
            throw new IllegalArgumentException("Unsupported query plan: " + nonNullPlan.getClass().getName());
        }
        requireCapabilities(nonNullPlan);
        if (nonNullPlan instanceof TimeSeriesQueryPlan<?>) {
            if (!(nonNullWindow instanceof TimeSeriesWindow timeSeriesWindow)) {
                throw new IllegalArgumentException("Time-series plans require a time-series window");
            }
            return timeSeriesPlan(asTimeSeries(nonNullPlan), timeSeriesWindow);
        }
        ProjectionQueryPlan<R> projectionPlan = asProjection(nonNullPlan);
        QueryProjection<R> projection = projectionPlan.getProjection();
        if (nonNullWindow instanceof KeysetWindow
                && (projectionPlan.containsAggregate() || !projectionPlan.getGroupBy().isEmpty())) {
            throw new IllegalArgumentException("Keyset windows are not supported for grouped query plans");
        }
        if (nonNullWindow instanceof TimeSeriesWindow
                && (projectionPlan.containsAggregate() || !projectionPlan.getGroupBy().isEmpty())) {
            throw new IllegalArgumentException("Time-series windows require a point query plan");
        }

        QueryCriteria effectiveCriteria = windowing.effectiveCriteria(projectionPlan.getCriteria(), nonNullWindow);
        Predicate<? super T> candidateFilter = windowing.candidateFilter(
                candidate -> true,
                effectiveCriteria,
                nonNullWindow
        );
        List<T> matchingCandidates = matchingCandidates(
                candidates(),
                candidateFilter,
                effectiveCriteria.getCondition()
        );
        windowing.order(matchingCandidates, effectiveCriteria.getOrders());

        List<PlanGroup<T>> groups = planGroups(matchingCandidates, projectionPlan);
        List<PlanRow<R, T>> rows = new ArrayList<>(groups.size());
        for (PlanGroup<T> group : groups) {
            @Nullable T representative = group.candidates().isEmpty()
                    ? null
                    : group.candidates().get(0);
            InMemoryQueryMatcher<T> matcher = new InMemoryQueryMatcher<>(
                    fieldResolver,
                    relationResolver,
                    relatedFieldResolvers,
                    relatedRelationResolvers,
                    functions,
                    operators,
                    aggregates,
                    representative,
                    group.candidates()
            );
            if (projectionPlan.getHaving() != null && !matcher.matches(projectionPlan.getHaving())) {
                continue;
            }
            List<Object> selected = new ArrayList<>(projection.getValues().size());
            for (QueryValue<?> value : projection.getValues()) {
                selected.add(matcher.evaluate(value));
            }
            R projected = projection.project(selected);
            rows.add(new PlanRow<>(projected, representative));
        }
        if (projectionPlan.isDistinct()) {
            Map<R, PlanRow<R, T>> distinctRows = new LinkedHashMap<>();
            for (PlanRow<R, T> row : rows) {
                distinctRows.putIfAbsent(row.item(), row);
            }
            rows = new ArrayList<>(distinctRows.values());
        }
        return projectedWindow(rows, effectiveCriteria.getOrders(), nonNullWindow);
    }

    private void requireCapabilities(Object root) {
        Deque<Object> pending = new ArrayDeque<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.push(root);
        while (!pending.isEmpty()) {
            Object node = pending.pop();
            if (!visited.add(node)) {
                continue;
            }
            if (node instanceof QueryExpression expression && !QueryExpression.isBuiltIn(expression)) {
                throw new IllegalArgumentException("Unsupported query expression: " + expression.getClass().getName());
            }
            if (node instanceof QueryValue<?> value && !QueryValue.isBuiltIn(value)) {
                throw new IllegalArgumentException("Unsupported query value: " + value.getClass().getName());
            }
            if (node instanceof QueryCriteria criteria) {
                pending.push(criteria.getCondition());
            } else if (node instanceof ProjectionQueryPlan<?> projection) {
                pending.push(projection.getCriteria());
                pending.addAll(projection.getProjection().getValues());
                pending.addAll(projection.getGroupBy());
                if (projection.getHaving() != null) {
                    pending.push(projection.getHaving());
                }
            } else if (node instanceof TimeSeriesQueryPlan<?> series) {
                pending.push(series.getCriteria());
                pending.push(series.getTimestamp());
                if (series.getValue() != null) {
                    pending.push(series.getValue());
                }
            } else if (node instanceof QueryCondition<?, ?> condition) {
                requireOperator(condition.getOperator());
            } else if (node instanceof QueryValueCondition<?, ?> condition) {
                requireOperator(condition.getOperator());
                pending.push(condition.getValue());
            } else if (node instanceof LogicalCondition condition) {
                pending.addAll(condition.getExpressions());
            } else if (node instanceof NegatedQueryExpression condition) {
                pending.push(condition.getExpression());
            } else if (node instanceof ExistsCondition<?> condition) {
                pending.push(condition.getCondition());
            } else if (node instanceof QueryValue.CustomFunction<?> function) {
                if (!functions.containsKey(function.getDefinition())) {
                    throw new IllegalArgumentException("In-memory query function is not registered: "
                            + function.getDefinition().getName());
                }
                pending.addAll(function.getArguments());
            } else if (node instanceof QueryValue.Function<?> function) {
                pending.addAll(function.getArguments());
            } else if (node instanceof QueryValue.Cast<?> cast) {
                pending.push(cast.getOperand());
            } else if (node instanceof QueryValue.Aggregate<?> aggregate) {
                if (aggregate.getFunction() instanceof QueryAggregateDefinition<?> definition
                        && !aggregates.containsKey(definition)) {
                    throw new IllegalArgumentException("In-memory query aggregate is not registered: " + definition.getName());
                }
                pending.addAll(aggregate.getArguments());
            }
        }
    }

    private void requireOperator(QueryOperator operator) {
        if (operator instanceof QueryOperatorDefinition<?, ?> definition) {
            if (!operators.containsKey(definition)) {
                throw new IllegalArgumentException("In-memory query operator is not registered: " + definition.getName());
            }
        } else if (!(operator instanceof QueryOperator.Standard)) {
            throw new IllegalArgumentException("Unsupported query operator");
        }
    }

    private <R> QueryResult<R> timeSeriesPlan(
            TimeSeriesQueryPlan<R> plan,
            TimeSeriesWindow window
    ) {
        Map<Instant, List<TimedValue>> grouped = new TreeMap<>();
        for (T candidate : candidates()) {
            T nonNullCandidate = Objects.requireNonNull(candidate, "candidate");
            InMemoryQueryMatcher<T> matcher = new InMemoryQueryMatcher<>(
                    fieldResolver,
                    relationResolver,
                    relatedFieldResolvers,
                    relatedRelationResolvers,
                    functions,
                    operators,
                    aggregates,
                    nonNullCandidate
            );
            if (!matcher.matches(plan.getCriteria().getCondition())) {
                continue;
            }
            Object resolvedTimestamp = matcher.evaluate(plan.getTimestamp());
            if (!(resolvedTimestamp instanceof Instant timestamp)) {
                throw new IllegalStateException("Time-series timestamp expression must resolve to an Instant");
            }
            if (!window.contains(timestamp)) {
                continue;
            }
            Object resolvedValue = plan.getValue() == null
                    ? null
                    : matcher.evaluate(plan.getValue());
            Instant bucketStart = plan.getBucket().start(timestamp);
            grouped.computeIfAbsent(bucketStart, ignored -> new ArrayList<>())
                    .add(new TimedValue(timestamp, resolvedValue));
        }
        List<R> points = new ArrayList<>(grouped.size());
        for (Map.Entry<Instant, List<TimedValue>> entry : grouped.entrySet()) {
            List<TimedValue> bucket = entry.getValue();
            bucket.sort(Comparator.comparing(TimedValue::timestamp));
            List<@Nullable Object> values = new ArrayList<>(bucket.size());
            for (TimedValue timedValue : bucket) {
                values.add(timedValue.value());
            }
            Object value = TimeSeriesAggregation.reduce(plan, values);
            if (value != null) {
                points.add(plan.point(entry.getKey(), value));
            }
        }
        int end = Math.min(points.size(), window.maximumPoints());
        return new SliceResult<>(
                points.subList(0, end),
                0L,
                window.maximumPoints(),
                end < points.size()
        );
    }

    private List<PlanGroup<T>> planGroups(List<T> candidates, ProjectionQueryPlan<?> plan) {
        if (!plan.getGroupBy().isEmpty()) {
            Map<List<Object>, List<T>> grouped = new LinkedHashMap<>();
            for (T candidate : candidates) {
                InMemoryQueryMatcher<T> matcher = matcher(candidate);
                List<Object> key = new ArrayList<>(plan.getGroupBy().size());
                for (QueryValue<?> value : plan.getGroupBy()) {
                    key.add(matcher.evaluate(value));
                }
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(candidate);
            }
            List<PlanGroup<T>> groups = new ArrayList<>(grouped.size());
            for (List<T> group : grouped.values()) {
                groups.add(new PlanGroup<>(group));
            }
            return groups;
        }
        if (plan.containsAggregate()) {
            return List.of(new PlanGroup<>(candidates));
        }
        List<PlanGroup<T>> groups = new ArrayList<>(candidates.size());
        for (T candidate : candidates) {
            groups.add(new PlanGroup<>(List.of(candidate)));
        }
        return groups;
    }

    @SuppressWarnings("unchecked")
    private static <R> ProjectionQueryPlan<R> asProjection(QueryPlan<R> plan) {
        return (ProjectionQueryPlan<R>) plan;
    }

    @SuppressWarnings("unchecked")
    private static <R> TimeSeriesQueryPlan<R> asTimeSeries(QueryPlan<R> plan) {
        return (TimeSeriesQueryPlan<R>) plan;
    }

    private QueryResult<T> query(
            Collection<? extends T> candidates,
            Predicate<? super T> candidateFilter,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        QueryCriteria effectiveCriteria = windowing.effectiveCriteria(criteria, window);
        Predicate<? super T> windowedCandidateFilter = windowing.candidateFilter(
                candidateFilter,
                effectiveCriteria,
                window
        );
        List<T> matchingCandidates = matchingCandidates(
                candidates,
                windowedCandidateFilter,
                effectiveCriteria.getCondition()
        );
        windowing.order(matchingCandidates, effectiveCriteria.getOrders());
        return windowing.apply(matchingCandidates, effectiveCriteria.getOrders(), window);
    }

    private Collection<? extends T> candidates() {
        return Objects.requireNonNull(candidateSupplier.get(), "candidateSupplier result");
    }

    private List<T> matchingCandidates(
            Collection<? extends T> candidates,
            Predicate<? super T> candidateFilter,
            QueryExpression condition
    ) {
        List<T> matches = new ArrayList<>();
        for (T candidate : candidates) {
            T nonNullCandidate = Objects.requireNonNull(candidate, "candidate");
            if (matcher(nonNullCandidate).matches(condition) && candidateFilter.test(nonNullCandidate)) {
                matches.add(nonNullCandidate);
            }
        }
        return matches;
    }

    private InMemoryQueryMatcher<T> matcher(@Nullable T candidate) {
        return new InMemoryQueryMatcher<>(
                fieldResolver,
                relationResolver,
                relatedFieldResolvers,
                relatedRelationResolvers,
                functions,
                operators,
                aggregates,
                candidate
        );
    }

    private <R> QueryResult<R> projectedWindow(
            List<PlanRow<R, T>> rows,
            List<OrderBy<?>> orders,
            QueryWindow window
    ) {
        if (window instanceof UnboundedWindow) {
            return new CompleteResult<>(items(rows));
        }
        if (window instanceof PageWindow pageWindow) {
            int start = InMemoryQueryWindow.startIndex(rows.size(), pageWindow.offset());
            int end = InMemoryQueryWindow.endIndex(start, pageWindow.limit(), rows.size());
            return new PageResult<>(
                    items(rows.subList(start, end)),
                    pageWindow.offset(),
                    pageWindow.limit(),
                    rows.size()
            );
        }
        if (window instanceof SliceWindow sliceWindow) {
            int start = InMemoryQueryWindow.startIndex(rows.size(), sliceWindow.offset());
            int end = InMemoryQueryWindow.endIndex(start, sliceWindow.limit(), rows.size());
            return new SliceResult<>(
                    items(rows.subList(start, end)),
                    sliceWindow.offset(),
                    sliceWindow.limit(),
                    end < rows.size()
            );
        }
        if (window instanceof KeysetWindow keysetWindow) {
            int end = Math.min(rows.size(), keysetWindow.limit());
            boolean hasNext = end < rows.size();
            List<PlanRow<R, T>> selected = rows.subList(0, end);
            KeysetPosition nextPosition = hasNext
                    ? windowing.position(selected.get(selected.size() - 1).representative(), orders)
                    : null;
            return new KeysetResult<>(items(selected), keysetWindow.limit(), nextPosition);
        }
        if (window instanceof TimeSeriesWindow timeSeriesWindow) {
            int end = Math.min(rows.size(), timeSeriesWindow.maximumPoints());
            return new SliceResult<>(
                    items(rows.subList(0, end)),
                    0L,
                    timeSeriesWindow.maximumPoints(),
                    end < rows.size()
            );
        }
        throw new IllegalArgumentException("Unsupported query window: " + window.getClass().getName());
    }

    private static <R, T> List<R> items(List<PlanRow<R, T>> rows) {
        List<R> items = new ArrayList<>(rows.size());
        for (PlanRow<R, T> row : rows) {
            items.add(row.item());
        }
        return items;
    }

    private static final class TimedValue {
        private final Instant timestamp;
        private final @Nullable Object value;

        private TimedValue(Instant timestamp, @Nullable Object value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        private Instant timestamp() {
            return timestamp;
        }

        private @Nullable Object value() {
            return value;
        }
    }

    private static final class PlanGroup<T> {
        private final List<T> candidates;

        private PlanGroup(List<T> candidates) {
            this.candidates = List.copyOf(candidates);
        }

        private List<T> candidates() {
            return candidates;
        }
    }

    private static final class PlanRow<R, T> {
        private final R item;
        private final @Nullable T representative;

        private PlanRow(R item, @Nullable T representative) {
            this.item = item;
            this.representative = representative;
        }

        private R item() {
            return item;
        }

        private @Nullable T representative() {
            return representative;
        }
    }
}
