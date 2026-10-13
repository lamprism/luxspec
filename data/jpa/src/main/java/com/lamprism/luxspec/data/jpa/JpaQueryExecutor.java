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

package com.lamprism.luxspec.data.jpa;

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
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.ProjectionQueryPlan;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExecutor;
import com.lamprism.luxspec.data.query.QueryPlan;
import com.lamprism.luxspec.data.query.QueryProjection;
import com.lamprism.luxspec.data.query.QueryValue;
import com.lamprism.luxspec.data.query.TimeSeriesAggregation;
import com.lamprism.luxspec.data.query.TimeSeriesPoint;
import com.lamprism.luxspec.data.query.TimeSeriesQueryPlan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Executes typed Luxspec criteria through a JPA entity manager.
 *
 * <p>The default constructor path maps query field names to direct entity attributes with the same
 * name. Applications remain responsible for validating criteria with their query schema and for
 * managing the entity manager transaction boundary. A {@link TimeSeriesWindow} expects the
 * standard {@link TimeSeriesPoint#TIMESTAMP} field to resolve to a non-null entity attribute.
 *
 * @param <T> the persistence entity type
 * @author RollW
 */
public class JpaQueryExecutor<T> implements QueryExecutor<T> {
    private final EntityManager entityManager;
    private final Class<T> entityType;
    private final JpaCriteriaTranslator translator;
    private final JpaQueryFieldResolver<T> fieldResolver;

    /**
     * Creates an executor with direct same-name entity attribute mapping.
     *
     * @param entityManager the entity manager used for query execution
     * @param entityType    the managed entity type
     */
    public JpaQueryExecutor(EntityManager entityManager, Class<T> entityType) {
        this(entityManager, entityType, new JpaCriteriaTranslator(), JpaQueryFieldResolver.byAttributeName());
    }

    /**
     * Creates an executor with a naming strategy for direct entity attributes.
     *
     * @param entityManager  the entity manager used for query execution
     * @param entityType     the managed entity type
     * @param namingStrategy the query-to-attribute naming strategy
     */
    public JpaQueryExecutor(
            EntityManager entityManager,
            Class<T> entityType,
            JpaFieldNamingStrategy namingStrategy
    ) {
        this(entityManager, entityType, new JpaCriteriaTranslator(), JpaQueryFieldResolver.byAttributeName(namingStrategy));
    }

    /**
     * Creates an executor with an explicit field resolver for complex mappings.
     *
     * <p>This is an advanced mapping extension point. Use the constructor that accepts only an
     * entity type for the standard direct attribute mapping.</p>
     *
     * @param entityManager the entity manager used for query execution
     * @param entityType    the managed entity type
     * @param fieldResolver the field-to-attribute resolver
     */
    public JpaQueryExecutor(
            EntityManager entityManager,
            Class<T> entityType,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        this(entityManager, entityType, new JpaCriteriaTranslator(), fieldResolver);
    }

    /**
     * Creates an executor with the supplied stateless translation collaborators.
     *
     * <p>This is an advanced mapping extension point. Standard callers can use the constructor
     * that accepts only an entity type.</p>
     *
     * @param entityManager the entity manager used for query execution
     * @param entityType    the managed entity type
     * @param translator    the Criteria translator
     * @param fieldResolver the field-to-attribute resolver
     */
    public JpaQueryExecutor(
            EntityManager entityManager,
            Class<T> entityType,
            JpaCriteriaTranslator translator,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.translator = Objects.requireNonNull(translator, "translator");
        this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
    }

    /**
     * Creates an executor with the supplied translator and direct attribute naming strategy.
     *
     * @param entityManager  the entity manager used for query execution
     * @param entityType     the managed entity type
     * @param translator     the Criteria translator
     * @param namingStrategy the query-to-attribute naming strategy
     */
    public JpaQueryExecutor(
            EntityManager entityManager,
            Class<T> entityType,
            JpaCriteriaTranslator translator,
            JpaFieldNamingStrategy namingStrategy
    ) {
        this(entityManager, entityType, translator, JpaQueryFieldResolver.byAttributeName(namingStrategy));
    }

    /**
     * Executes criteria using the requested result window.
     *
     * <p>A page performs an exact count query. A slice fetches one additional row to determine
     * whether another slice exists. An unbounded window returns all matching rows.
     *
     * @param criteria structured conditions and ordering
     * @param window   requested result window
     * @return the complete, page, or slice result
     */
    @Override
    public QueryResult<T> query(QueryCriteria criteria, QueryWindow window) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        if (nonNullWindow instanceof UnboundedWindow) {
            return new CompleteResult<>(execute(nonNullCriteria, 0, null));
        }
        if (nonNullWindow instanceof PageWindow pageWindow) {
            return page(nonNullCriteria, pageWindow);
        }
        if (nonNullWindow instanceof SliceWindow sliceWindow) {
            return slice(nonNullCriteria, sliceWindow);
        }
        if (nonNullWindow instanceof KeysetWindow keysetWindow) {
            return keyset(nonNullCriteria, keysetWindow);
        }
        if (nonNullWindow instanceof TimeSeriesWindow timeSeriesWindow) {
            return timeSeries(nonNullCriteria, timeSeriesWindow);
        }
        throw new IllegalArgumentException("Unsupported query window: " + nonNullWindow.getClass().getName());
    }

    /**
     * Executes a typed projection, aggregate, or time-series plan through JPA Criteria.
     *
     * @param plan   the validated query plan
     * @param window the requested result window
     * @param <R>    the plan result type
     * @return the plan result
     */
    @Override
    public <R> QueryResult<R> query(QueryPlan<R> plan, QueryWindow window) {
        QueryPlan<R> nonNullPlan = Objects.requireNonNull(plan, "plan");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        if (nonNullPlan instanceof TimeSeriesQueryPlan<?>) {
            if (!(nonNullWindow instanceof TimeSeriesWindow timeSeriesWindow)) {
                throw new IllegalArgumentException("Time-series plans require a time-series window");
            }
            return timeSeriesPlan(asTimeSeries(nonNullPlan), timeSeriesWindow);
        }
        ProjectionQueryPlan<R> projectionPlan = asProjection(nonNullPlan);
        if (nonNullWindow instanceof KeysetWindow) {
            if (projectionPlan.containsAggregate() || !projectionPlan.getGroupBy().isEmpty()) {
                throw new IllegalArgumentException("Keyset windows are not supported for grouped query plans");
            }
            return keysetPlan(projectionPlan, (KeysetWindow) nonNullWindow);
        }
        if (nonNullWindow instanceof TimeSeriesWindow
                && (projectionPlan.containsAggregate() || !projectionPlan.getGroupBy().isEmpty())) {
            throw new IllegalArgumentException("Time-series windows require a point query plan");
        }
        if (nonNullWindow instanceof UnboundedWindow) {
            return new CompleteResult<>(executePlan(projectionPlan, 0, null, null));
        }
        if (nonNullWindow instanceof PageWindow pageWindow) {
            List<R> items = executePlan(projectionPlan, toJpaOffset(pageWindow.offset()), pageWindow.limit(), null);
            long total = executePlan(projectionPlan, 0, null, null).size();
            return new PageResult<>(items, pageWindow.offset(), pageWindow.limit(), total);
        }
        if (nonNullWindow instanceof SliceWindow sliceWindow) {
            List<R> fetched = executePlan(
                    projectionPlan,
                    toJpaOffset(sliceWindow.offset()),
                    fetchLimit(sliceWindow.limit()),
                    null
            );
            return sliceResult(fetched, sliceWindow.offset(), sliceWindow.limit());
        }
        if (nonNullWindow instanceof TimeSeriesWindow timeSeriesWindow) {
            List<R> fetched = executePlan(
                    projectionPlan,
                    0,
                    fetchLimit(timeSeriesWindow.maximumPoints()),
                    timeSeriesWindow
            );
            return sliceResult(fetched, 0L, timeSeriesWindow.maximumPoints());
        }
        throw new IllegalArgumentException("Unsupported query window: " + nonNullWindow.getClass().getName());
    }

    private <R> QueryResult<R> timeSeriesPlan(
            TimeSeriesQueryPlan<R> plan,
            TimeSeriesWindow window
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<T> root = query.from(entityType);
        @SuppressWarnings("unchecked")
        Expression<Instant> timestamp = (Expression<Instant>) translator.value(
                builder,
                root,
                query,
                plan.getTimestamp(),
                fieldResolver
        );
        List<Selection<?>> selections = new ArrayList<>();
        selections.add(timestamp);
        if (plan.getValue() != null) {
            selections.add(translator.value(
                    builder,
                    root,
                    query,
                    plan.getValue(),
                    fieldResolver
            ));
        }
        query.select(builder.tuple(selections));
        Predicate predicate = translator.predicate(
                builder,
                root,
                query,
                plan.getCriteria().getCondition(),
                fieldResolver
        );
        query.where(builder.and(
                predicate,
                builder.greaterThanOrEqualTo(timestamp, window.fromInclusive()),
                builder.lessThanOrEqualTo(timestamp, window.toInclusive())
        ));
        query.orderBy(builder.asc(timestamp));
        List<Tuple> rows = entityManager.createQuery(query).getResultList();
        Map<Instant, List<Object>> grouped = new TreeMap<>();
        for (Tuple row : rows) {
            Object rawTimestamp = row.get(0);
            if (!(rawTimestamp instanceof Instant resolvedTimestamp)) {
                throw new IllegalStateException("Time-series timestamp expression must resolve to an Instant");
            }
            Object resolvedValue = plan.getValue() == null ? null : row.get(1);
            Instant bucketStart = plan.getBucket().start(resolvedTimestamp);
            grouped.computeIfAbsent(bucketStart, ignored -> new ArrayList<>()).add(resolvedValue);
        }
        List<R> points = new ArrayList<>(grouped.size());
        for (Map.Entry<Instant, List<Object>> entry : grouped.entrySet()) {
            Object value = TimeSeriesAggregation.reduce(plan, entry.getValue());
            if (value != null) {
                points.add(plan.point(entry.getKey(), value));
            }
        }
        return sliceResult(points, 0L, window.maximumPoints());
    }

    private <R> List<R> executePlan(
            ProjectionQueryPlan<R> plan,
            int offset,
            @Nullable Integer maxResults,
            @Nullable TimeSeriesWindow timeSeriesWindow
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<T> root = query.from(entityType);
        QueryProjection<R> projection = plan.getProjection();
        QueryCriteria criteria = effectiveCriteria(plan.getCriteria(), timeSeriesWindow);
        query.select(builder.tuple(selectValues(builder, root, query, projection.getValues())));
        query.distinct(plan.isDistinct());

        Predicate predicate = translator.predicate(
                builder,
                root,
                query,
                criteria.getCondition(),
                fieldResolver
        );
        List<Order> orders = translator.orders(builder, root, criteria, fieldResolver);
        if (timeSeriesWindow != null) {
            Expression<Instant> timestamp = fieldResolver.resolve(root, TimeSeriesPoint.TIMESTAMP);
            predicate = builder.and(
                    predicate,
                    builder.greaterThanOrEqualTo(timestamp, timeSeriesWindow.fromInclusive()),
                    builder.lessThanOrEqualTo(timestamp, timeSeriesWindow.toInclusive())
            );
            if (orders.isEmpty()) {
                orders = List.of(builder.asc(timestamp));
            }
        }
        query.where(predicate);
        if (!plan.getGroupBy().isEmpty()) {
            List<Expression<?>> groupBy = new ArrayList<>();
            for (QueryValue<?> value : plan.getGroupBy()) {
                groupBy.add(translator.value(builder, root, query, value, fieldResolver));
            }
            query.groupBy(groupBy);
        }
        if (plan.getHaving() != null) {
            query.having(translator.predicate(
                    builder,
                    root,
                    query,
                    plan.getHaving(),
                    fieldResolver
            ));
        }
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }
        if (maxResults != null) {
            typedQuery.setMaxResults(maxResults);
        }
        List<R> results = new ArrayList<>();
        for (Tuple tuple : typedQuery.getResultList()) {
            results.add(projection.project(selectedValues(projection, tuple)));
        }
        return List.copyOf(results);
    }

    private <R> KeysetResult<R> keysetPlan(ProjectionQueryPlan<R> plan, KeysetWindow window) {
        QueryCriteria criteria = plan.getCriteria();
        requireKeysetOrdering(criteria, window);
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<T> root = query.from(entityType);
        QueryProjection<R> projection = plan.getProjection();
        List<Selection<?>> selections = selectValues(builder, root, query, projection.getValues());
        for (OrderBy<?> order : criteria.getOrders()) {
            selections.add(fieldResolver.resolve(root, order.getField()));
        }
        query.select(builder.tuple(selections));
        query.distinct(plan.isDistinct());
        Predicate predicate = translator.predicate(
                builder,
                root,
                query,
                criteria.getCondition(),
                fieldResolver
        );
        if (window.after() != null) {
            predicate = builder.and(
                    predicate,
                    translator.keysetPredicate(builder, root, criteria, window.after(), fieldResolver)
            );
        }
        query.where(predicate);
        List<Order> orders = translator.orders(builder, root, criteria, fieldResolver);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(fetchLimit(window.limit()));
        List<KeysetRow<R>> fetched = new ArrayList<>();
        for (Tuple tuple : typedQuery.getResultList()) {
            List<Object> selected = selectedValues(projection, tuple);
            fetched.add(new KeysetRow<>(
                    projection.project(selected),
                    keysetPosition(tuple, projection.getValues().size(), criteria.getOrders())
            ));
        }
        return keysetResult(fetched, window.limit());
    }

    private static QueryCriteria effectiveCriteria(
            QueryCriteria criteria,
            @Nullable TimeSeriesWindow timeSeriesWindow
    ) {
        if (timeSeriesWindow == null || !criteria.getOrders().isEmpty()) {
            return criteria;
        }
        return new QueryCriteria(
                criteria.getCondition(),
                List.of(OrderBy.ascending(TimeSeriesPoint.TIMESTAMP))
        );
    }

    private List<Selection<?>> selectValues(
            CriteriaBuilder builder,
            Root<T> root,
            CriteriaQuery<Tuple> query,
            List<? extends QueryValue<?>> values
    ) {
        List<Selection<?>> selections = new ArrayList<>(values.size());
        for (QueryValue<?> value : values) {
            selections.add(translator.value(builder, root, query, value, fieldResolver));
        }
        return selections;
    }

    private static <R> List<Object> selectedValues(QueryProjection<R> projection, Tuple tuple) {
        List<Object> selected = new ArrayList<>(projection.getValues().size());
        for (int index = 0; index < projection.getValues().size(); index++) {
            selected.add(tuple.get(index));
        }
        return selected;
    }

    private static KeysetPosition keysetPosition(
            Tuple tuple,
            int valueOffset,
            List<? extends OrderBy<?>> orders
    ) {
        List<Object> position = new ArrayList<>(orders.size());
        for (int index = 0; index < orders.size(); index++) {
            Object value = tuple.get(valueOffset + index);
            if (value == null) {
                throw new IllegalStateException(
                        "Keyset order field must not resolve to null: "
                                + orders.get(index).getField().getName()
                );
            }
            position.add(value);
        }
        return new KeysetPosition(position);
    }

    private static <R> KeysetResult<R> keysetResult(
            List<KeysetRow<R>> fetched,
            int limit
    ) {
        boolean hasNext = fetched.size() > limit;
        List<KeysetRow<R>> selected = hasNext
                ? new ArrayList<>(fetched.subList(0, limit))
                : fetched;
        List<R> items = selected.stream().map(KeysetRow::item).toList();
        KeysetPosition nextPosition = hasNext
                ? selected.get(selected.size() - 1).position()
                : null;
        return new KeysetResult<>(items, limit, nextPosition);
    }

    private static <R> SliceResult<R> sliceResult(List<R> fetched, long offset, int limit) {
        boolean hasNext = fetched.size() > limit;
        List<R> items = hasNext
                ? new ArrayList<>(fetched.subList(0, limit))
                : fetched;
        return new SliceResult<>(items, offset, limit, hasNext);
    }

    @SuppressWarnings("unchecked")
    private static <R> ProjectionQueryPlan<R> asProjection(QueryPlan<R> plan) {
        return (ProjectionQueryPlan<R>) plan;
    }

    @SuppressWarnings("unchecked")
    private static <R> TimeSeriesQueryPlan<R> asTimeSeries(QueryPlan<R> plan) {
        return (TimeSeriesQueryPlan<R>) plan;
    }

    /**
     * Finds the only entity matching criteria without executing a count query.
     *
     * <p>The query is limited to two rows. More than one matching row raises
     * {@link NonUniqueResultException}; no row returns {@link Optional#empty()}.
     *
     * @param criteria structured conditions and ordering
     * @return the matching entity when exactly one exists
     * @throws NonUniqueResultException when multiple entities match
     */
    public Optional<T> findOne(QueryCriteria criteria) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        List<T> items = execute(nonNullCriteria, 0, 2);
        if (items.size() > 1) {
            throw new NonUniqueResultException("More than one entity matches the query criteria");
        }
        return items.stream().findFirst();
    }

    private PageResult<T> page(QueryCriteria criteria, PageWindow window) {
        int offset = toJpaOffset(window.offset());
        List<T> items = execute(criteria, offset, window.limit());
        long total = count(criteria);
        return new PageResult<>(items, window.offset(), window.limit(), total);
    }

    private SliceResult<T> slice(QueryCriteria criteria, SliceWindow window) {
        int offset = toJpaOffset(window.offset());
        int fetchLimit = fetchLimit(window.limit());
        List<T> fetchedItems = execute(criteria, offset, fetchLimit);
        return sliceResult(fetchedItems, window.offset(), window.limit());
    }

    private SliceResult<T> timeSeries(QueryCriteria criteria, TimeSeriesWindow window) {
        int fetchLimit = fetchLimit(window.maximumPoints());
        List<T> fetchedItems = execute(criteria, 0, fetchLimit, window, null);
        return sliceResult(fetchedItems, 0L, window.maximumPoints());
    }

    private KeysetResult<T> keyset(QueryCriteria criteria, KeysetWindow window) {
        requireKeysetOrdering(criteria, window);
        int fetchLimit = fetchLimit(window.limit());
        List<KeysetRow<T>> fetchedRows = executeKeyset(criteria, window.after(), fetchLimit);
        return keysetResult(fetchedRows, window.limit());
    }

    private List<T> execute(QueryCriteria criteria, int offset, @Nullable Integer maxResults) {
        return execute(criteria, offset, maxResults, null, null);
    }

    private List<T> execute(
            QueryCriteria criteria,
            int offset,
            @Nullable Integer maxResults,
            @Nullable TimeSeriesWindow timeSeriesWindow,
            @Nullable KeysetPosition keysetPosition
    ) {
        TypedQuery<T> query = createSelectionQuery(criteria, timeSeriesWindow, keysetPosition);
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }

    private List<KeysetRow<T>> executeKeyset(
            QueryCriteria criteria,
            @Nullable KeysetPosition position,
            int maxResults
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<T> root = query.from(entityType);
        List<Selection<?>> selections = new ArrayList<>();
        selections.add(root);
        for (OrderBy<?> order : criteria.getOrders()) {
            selections.add(fieldResolver.resolve(root, order.getField()));
        }
        query.select(builder.tuple(selections));
        Predicate predicate = translator.predicate(
                builder,
                root,
                query,
                criteria.getCondition(),
                fieldResolver
        );
        if (position != null) {
            predicate = builder.and(
                    predicate,
                    translator.keysetPredicate(builder, root, criteria, position, fieldResolver)
            );
        }
        query.where(predicate);
        List<Order> orders = translator.orders(builder, root, criteria, fieldResolver);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(maxResults);
        List<KeysetRow<T>> rows = new ArrayList<>();
        for (Tuple tuple : typedQuery.getResultList()) {
            @SuppressWarnings("unchecked")
            T item = (T) tuple.get(0);
            rows.add(new KeysetRow<>(
                    item,
                    keysetPosition(tuple, 1, criteria.getOrders())
            ));
        }
        return List.copyOf(rows);
    }

    private TypedQuery<T> createSelectionQuery(
            QueryCriteria criteria,
            @Nullable TimeSeriesWindow timeSeriesWindow,
            @Nullable KeysetPosition keysetPosition
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityType);
        Root<T> root = query.from(entityType);
        query.select(root);
        Predicate predicate = translator.predicate(
                builder,
                root,
                query,
                criteria.getCondition(),
                fieldResolver
        );
        if (keysetPosition != null) {
            predicate = builder.and(
                    predicate,
                    translator.keysetPredicate(builder, root, criteria, keysetPosition, fieldResolver)
            );
        }
        List<Order> orders = translator.orders(builder, root, criteria, fieldResolver);
        if (timeSeriesWindow != null) {
            Expression<Instant> timestamp = fieldResolver.resolve(root, TimeSeriesPoint.TIMESTAMP);
            predicate = builder.and(
                    predicate,
                    builder.greaterThanOrEqualTo(timestamp, timeSeriesWindow.fromInclusive()),
                    builder.lessThanOrEqualTo(timestamp, timeSeriesWindow.toInclusive())
            );
            if (orders.isEmpty()) {
                orders = List.of(builder.asc(timestamp));
            }
        }
        query.where(predicate);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        return entityManager.createQuery(query);
    }

    private static void requireKeysetOrdering(QueryCriteria criteria, KeysetWindow window) {
        if (criteria.getOrders().isEmpty()) {
            throw new IllegalArgumentException("Keyset windows require explicit query ordering");
        }
        if (window.after() != null
                && window.after().values().size() != criteria.getOrders().size()) {
            throw new IllegalArgumentException("Keyset position does not match query ordering");
        }
    }

    private long count(QueryCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<T> root = query.from(entityType);
        query.select(builder.count(root));
        query.where(translator.predicate(
                builder,
                root,
                query,
                criteria.getCondition(),
                fieldResolver
        ));
        return entityManager.createQuery(query).getSingleResult();
    }

    private static int toJpaOffset(long offset) {
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Query offset exceeds the JPA integer range");
        }
        return (int) offset;
    }

    private static int fetchLimit(int limit) {
        if (limit == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Slice limit is too large to detect continuation");
        }
        return limit + 1;
    }

    private record KeysetRow<T>(T item, KeysetPosition position) {
    }
}
