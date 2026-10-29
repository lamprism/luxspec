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
import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.ExistsCondition;
import com.lamprism.luxspec.data.query.LikeCondition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryAggregateDefinition;
import com.lamprism.luxspec.data.query.QueryAggregateFunction;
import com.lamprism.luxspec.data.query.QueryComplexityBudget;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryExpressionVisitor;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryFunction;
import com.lamprism.luxspec.data.query.QueryFunctionDefinition;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QueryOperatorDefinition;
import com.lamprism.luxspec.data.query.QueryPlan;
import com.lamprism.luxspec.data.query.QueryProjection;
import com.lamprism.luxspec.data.query.QueryRelation;
import com.lamprism.luxspec.data.query.QuerySchema;
import com.lamprism.luxspec.data.query.QueryValue;
import com.lamprism.luxspec.data.query.QueryValueCondition;
import com.lamprism.luxspec.data.query.QueryValueVisitor;
import com.lamprism.luxspec.data.query.TimeSeriesAggregationFunction;
import com.lamprism.luxspec.data.query.TimeSeriesBucket;
import com.lamprism.luxspec.data.query.TimeSeriesPoint;
import com.lamprism.luxspec.naming.CaseFormat;
import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationView;
import com.lamprism.luxspec.observability.runtime.instrumentation.ObservabilityInstrumentation;
import com.lamprism.luxspec.observability.runtime.observation.DatabaseObservationAttributes;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationSet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class JpaCriteriaTranslatorIntegrationTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<Integer> NAME_LENGTH = QueryField.of("nameLength", Integer.class);
    private static final QueryField<String> CATEGORY = QueryField.of("category", String.class);
    private static final QueryField<Integer> SCORE = QueryField.of("score", Integer.class);
    private static final QueryField<String> OPTIONAL_VALUE = QueryField.of("optionalValue", String.class);
    private static final QueryField<String> DISPLAY_NAME = QueryField.of("displayName", String.class);
    private static final QueryField<String> DISPLAY_NAME_SNAKE = QueryField.of("display_name", String.class);

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @Test
    void observesJpaQueryExecutionUsingBoundedAttributes() {
        persistItems(new QueryableItemEntity("alpha", "active", 10));
        AtomicReference<ObservationView> stopped = new AtomicReference<>();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStop(ObservationView view) {
                stopped.set(view);
            }
        };
        try (ObservationRegistry registry = ObservationRegistryBuilder.builder()
                .set(StandardObservationSet.builder().domain(StandardObservationSet.Domain.DATABASE).build())
                .handler(handler).build()) {
            var observed = ObservabilityInstrumentation.queryExecutor(executor(), registry);
            assertEquals(1, observed.query(QueryCriteria.empty(), UnboundedWindow.getInstance()).getItems().size());
        }
        assertEquals(DatabaseObservationAttributes.Operation.QUERY,
                stopped.get().attributes().get(DatabaseObservationAttributes.OPERATION).orElseThrow());
        assertEquals(1, stopped.get().attributes().values().size());
    }

    @BeforeEach
    void openPersistenceContext() {
        entityManagerFactory = Persistence.createEntityManagerFactory("luxspec-data-jpa-test");
        entityManager = entityManagerFactory.createEntityManager();
    }

    @AfterEach
    void closePersistenceContext() {
        if (entityManager != null) {
            entityManager.close();
        }
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void translatesNestedConditionsAndDescendingOrderAgainstJpa() {
        persistItems(
                new QueryableItemEntity("adam", "active", 10),
                new QueryableItemEntity("alicia", "active", 30),
                new QueryableItemEntity("albert", "blocked", 50),
                new QueryableItemEntity("bruno", "active", 100)
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.and(List.of(
                        LikeCondition.of(NAME, "a%"),
                        ComparisonCondition.greaterThanOrEqualTo(SCORE, 10),
                        QueryExpression.not(QueryCondition.equal(CATEGORY, "blocked"))
                )),
                List.of(OrderBy.descending(SCORE))
        );

        List<String> names = query(criteria).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of("alicia", "adam"), names);
    }

    @Test
    void resolvesAnApplicationDefinedDerivedFieldThroughCriteria() {
        persistItems(new QueryableItemEntity("alpha", "active", 1), new QueryableItemEntity("beta", "active", 2));
        QueryCriteria criteria = new QueryCriteria(QueryCondition.equal(NAME_LENGTH, 5), List.of());
        QuerySchema.builder().add(NAME_LENGTH, Set.of(QueryOperator.EQUAL)).build()
                .validate(criteria, QueryComplexityBudget.defaults());
        JpaQueryFieldResolver<QueryableItemEntity> resolver = new JpaQueryFieldResolver<>() {
            @Override
            public <V> Expression<V> resolve(Root<QueryableItemEntity> root, QueryField<V> field) {
                if (NAME_LENGTH.equals(field)) {
                    return entityManager.getCriteriaBuilder().length(root.get("name")).as(field.getValueType());
                }
                return JpaQueryFieldResolver.<QueryableItemEntity>byAttributeName().resolve(root, field);
            }
        };

        assertEquals(List.of("alpha"), query(criteria, resolver).stream().map(QueryableItemEntity::name).toList());
    }

    @Test
    void translatesMembershipAndDisjunctionAgainstJpa() {
        persistItems(
                new QueryableItemEntity("alpha", "priority", 1),
                new QueryableItemEntity("bravo", "archived", 2),
                new QueryableItemEntity("charlie", "standard", 3),
                new QueryableItemEntity("delta", "archived", 4)
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.or(List.of(
                        QueryCondition.in(CATEGORY, List.of("priority", "standard")),
                        QueryCondition.notIn(NAME, List.of("alpha", "charlie", "delta"))
                )),
                List.of(OrderBy.ascending(NAME))
        );

        List<String> names = query(criteria).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of("alpha", "bravo", "charlie"), names);
    }

    @Test
    void translatesNullPredicatesAgainstJpa() {
        persistItems(
                new QueryableItemEntity("alpha", "priority", 1),
                new QueryableItemEntity("bravo", "standard", 2)
        );

        List<String> nullNames = query(new QueryCriteria(
                QueryCondition.isNull(NAME),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();
        List<String> nonNullNames = query(new QueryCriteria(
                QueryCondition.isNotNull(NAME),
                List.of(OrderBy.ascending(NAME))
        )).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of(), nullNames);
        assertEquals(List.of("alpha", "bravo"), nonNullNames);
    }

    @Test
    void keepsNegativePredicatesTwoValuedForNullFieldsLikeInMemoryQueries() {
        persistItems(
                new QueryableItemEntity("empty", "standard", 1, (String) null),
                new QueryableItemEntity("present", "standard", 2, "value")
        );

        List<String> notEqualNames = query(new QueryCriteria(
                QueryCondition.notEqual(OPTIONAL_VALUE, "value"),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();
        List<String> notInNames = query(new QueryCriteria(
                QueryCondition.notIn(OPTIONAL_VALUE, List.of("value")),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();
        List<String> negatedEqualNames = query(new QueryCriteria(
                QueryExpression.not(QueryCondition.equal(OPTIONAL_VALUE, "value")),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of(), notEqualNames);
        assertEquals(List.of(), notInNames);
        assertEquals(List.of("empty"), negatedEqualNames);
    }

    @Test
    void appliesRegisteredCustomOperatorsInsideJpaExists() {
        QueryableItemEntity matching = new QueryableItemEntity("alpha", "active", 1);
        matching.addTag("administrator");
        QueryableItemEntity nonMatching = new QueryableItemEntity("beta", "active", 2);
        nonMatching.addTag("staff");
        persistItems(matching, nonMatching);
        QueryField<String> label = QueryField.of("label", String.class);
        QueryRelation<QueryableTagEntity> tags = QueryRelation.toMany("tags", QueryableTagEntity.class);
        QueryOperatorDefinition<String, Integer> minimum = QueryOperatorDefinition.of(
                "minimumLength", String.class, Integer.class, 1, 1
        );
        QueryCriteria criteria = new QueryCriteria(
                ExistsCondition.of(tags, QueryCondition.of(label, minimum, List.of(8))), List.of()
        );
        QuerySchema.builder().relation(tags, QuerySchema.builder().add(label, Set.of(minimum)).build())
                .build().validate(criteria, QueryComplexityBudget.defaults());
        JpaCriteriaTranslator translator = JpaCriteriaTranslator.builder()
                .operator(minimum, (builder, value, arguments) -> builder.and(builder.isNotNull(value),
                        builder.greaterThanOrEqualTo(builder.length(value), arguments.get(0))))
                .build();
        JpaQueryExecutor<QueryableItemEntity> executor = new JpaQueryExecutor<>(
                entityManager, QueryableItemEntity.class, translator, JpaQueryFieldResolver.byAttributeName()
        );

        assertEquals(List.of("alpha"), executor.query(criteria, UnboundedWindow.getInstance())
                .getItems().stream().map(QueryableItemEntity::name).toList());
        assertThrows(IllegalArgumentException.class,
                () -> executor().query(criteria, UnboundedWindow.getInstance()));
    }

    @Test
    void executesRegisteredCustomOperatorsAgainstJpa() {
        persistItems(new QueryableItemEntity("alpha", "active", 1, (String) null),
                new QueryableItemEntity("be", "active", 2, "present"));
        QueryOperatorDefinition<String, Integer> minimum = QueryOperatorDefinition.of(
                "minimumLength", String.class, Integer.class, 1, 1
        );
        QueryOperatorDefinition<String, Void> absent = QueryOperatorDefinition.of("absent", String.class, Void.class, 0, 0);
        QuerySchema schema = QuerySchema.builder()
                .add(NAME, Set.of(minimum), false, Set.of(QueryFunction.LOWER))
                .add(OPTIONAL_VALUE, Set.of(absent, minimum))
                .build();
        JpaCriteriaTranslator translator = JpaCriteriaTranslator.builder()
                .operator(minimum, (builder, value, arguments) ->
                        builder.and(builder.isNotNull(value),
                                builder.greaterThanOrEqualTo(builder.length(value), arguments.get(0))))
                .operator(absent, (builder, value, arguments) -> builder.isNull(value))
                .build();
        JpaQueryExecutor<QueryableItemEntity> executor = new JpaQueryExecutor<>(
                entityManager, QueryableItemEntity.class, translator, JpaQueryFieldResolver.byAttributeName()
        );
        QueryCriteria criteria = new QueryCriteria(QueryExpression.and(List.of(
                QueryValueCondition.of(QueryValue.lower(NAME), minimum, List.of(4)),
                QueryCondition.of(OPTIONAL_VALUE, absent, List.of())
        )), List.of());

        schema.validate(criteria, QueryComplexityBudget.defaults());
        assertEquals(List.of("alpha"), executor.query(criteria, UnboundedWindow.getInstance())
                .getItems().stream().map(QueryableItemEntity::name).toList());
        assertThrows(IllegalArgumentException.class,
                () -> executor().query(criteria, UnboundedWindow.getInstance()));
        QueryCriteria negated = new QueryCriteria(
                QueryExpression.not(QueryCondition.of(OPTIONAL_VALUE, minimum, List.of(4))), List.of()
        );
        schema.validate(negated, QueryComplexityBudget.defaults());
        assertEquals(List.of("alpha"), executor.query(negated, UnboundedWindow.getInstance())
                .getItems().stream().map(QueryableItemEntity::name).toList());

        QueryOperatorDefinition<Long, Long> minimumCount = QueryOperatorDefinition.of(
                "minimumCount", Long.class, Long.class, 1, 1
        );
        JpaQueryExecutor<QueryableItemEntity> aggregateExecutor = new JpaQueryExecutor<>(
                entityManager, QueryableItemEntity.class,
                JpaCriteriaTranslator.builder().operator(minimumCount,
                        (builder, value, arguments) -> builder.greaterThanOrEqualTo(value, arguments.get(0))).build(),
                JpaQueryFieldResolver.byAttributeName()
        );
        QueryPlan<Long> plan = QueryPlan.builder(QueryValue.count())
                .having(QueryValueCondition.of(QueryValue.count(), minimumCount, List.of(2L))).build();
        QuerySchema.builder().operator(minimumCount).build().validate(plan, QueryComplexityBudget.defaults());
        assertEquals(List.of(2L), aggregateExecutor.query(plan, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void translatesRegisteredNestedFunctionsAndRejectsMissingImplementations() {
        persistItems(new QueryableItemEntity("  alpha ", "active", 1),
                new QueryableItemEntity("beta", "active", 2));
        QueryFunctionDefinition<String> trim = QueryFunctionDefinition.of("trim", String.class, String.class);
        QueryValue<String> normalized = QueryValue.upper(QueryValue.function(trim, QueryValue.field(NAME)));
        QueryCriteria criteria = new QueryCriteria(QueryValueCondition.equal(normalized, "ALPHA"), List.of());
        QuerySchema.builder()
                .add(NAME, Set.of(QueryOperator.EQUAL), false, Set.of(QueryFunction.UPPER))
                .function(trim, Set.of(NAME))
                .build()
                .validate(criteria, QueryComplexityBudget.defaults());
        JpaCriteriaTranslator translator = JpaCriteriaTranslator.builder()
                .function(trim, (builder, arguments) -> builder.trim(arguments.get(0).as(String.class)))
                .build();
        JpaQueryExecutor<QueryableItemEntity> customExecutor = new JpaQueryExecutor<>(
                entityManager, QueryableItemEntity.class, translator, JpaQueryFieldResolver.byAttributeName()
        );

        assertEquals(List.of("  alpha "), customExecutor.query(criteria, UnboundedWindow.getInstance())
                .getItems().stream().map(QueryableItemEntity::name).toList());
        assertThrows(IllegalArgumentException.class,
                () -> executor().query(criteria, UnboundedWindow.getInstance()));

        QueryPlan<Long> aggregate = QueryPlan.builder(QueryValue.count(QueryValue.function(trim, QueryValue.field(NAME))))
                .build();
        QuerySchema.builder()
                .add(NAME, Set.of(QueryOperator.EQUAL), false, Set.of(), Set.of(QueryAggregateFunction.COUNT))
                .function(trim, Set.of(NAME)).build().validate(aggregate, QueryComplexityBudget.defaults());
        assertEquals(List.of(2L), customExecutor.query(aggregate, UnboundedWindow.getInstance()).getItems());

        QueryFunctionDefinition<Long> identity = QueryFunctionDefinition.of("identity", Long.class, Long.class);
        JpaCriteriaTranslator aggregateTranslator = JpaCriteriaTranslator.builder()
                .function(identity, (builder, arguments) -> arguments.get(0).as(Long.class)).build();
        JpaQueryExecutor<QueryableItemEntity> aggregateExecutor = new JpaQueryExecutor<>(
                entityManager, QueryableItemEntity.class, aggregateTranslator, JpaQueryFieldResolver.byAttributeName()
        );
        QueryPlan<Long> wrappedAggregate = QueryPlan.builder(QueryValue.function(identity, QueryValue.count())).build();
        QuerySchema.builder().function(identity, Set.of()).build()
                .validate(wrappedAggregate, QueryComplexityBudget.defaults());
        assertEquals(List.of(2L), aggregateExecutor.query(wrappedAggregate, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void translatesValueFunctionsAndCastsAgainstJpa() {
        persistItems(
                new QueryableItemEntity("Alpha", "priority", 10),
                new QueryableItemEntity("bravo", "standard", 20)
        );

        List<String> lowerNames = query(new QueryCriteria(
                QueryValueCondition.like(QueryValue.lower(NAME), "a%"),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();
        List<String> upperNames = query(new QueryCriteria(
                QueryValueCondition.equal(QueryValue.upper(NAME), "BRAVO"),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();
        List<String> coalesceNames = query(new QueryCriteria(
                QueryValueCondition.equal(QueryValue.coalesce(NAME, "fallback"), "Alpha"),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();
        List<String> castNames = query(new QueryCriteria(
                QueryValueCondition.like(QueryValue.cast(SCORE, String.class), "2%"),
                List.of()
        )).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of("Alpha"), lowerNames);
        assertEquals(List.of("bravo"), upperNames);
        assertEquals(List.of("Alpha"), coalesceNames);
        assertEquals(List.of("bravo"), castNames);
    }

    @Test
    void executesPageAndSliceWithoutManualCriteriaSetup() {
        persistItems(
                new QueryableItemEntity("charlie", "standard", 3),
                new QueryableItemEntity("alpha", "priority", 1),
                new QueryableItemEntity("bravo", "standard", 2)
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.all(),
                List.of(OrderBy.ascending(NAME))
        );

        QueryResult<QueryableItemEntity> page = executor().query(criteria, new PageWindow(1, 1));
        assertTrue(page instanceof PageResult<?>);
        assertEquals(List.of("bravo"), names(page));
        assertEquals(3L, ((PageResult<QueryableItemEntity>) page).total());

        QueryResult<QueryableItemEntity> slice = executor().query(criteria, new SliceWindow(0, 2));
        assertTrue(slice instanceof SliceResult<?>);
        assertEquals(List.of("alpha", "bravo"), names(slice));
        assertTrue(((SliceResult<QueryableItemEntity>) slice).hasNext());
    }

    @Test
    void executesKeysetWindowsThroughTheRegularJpaQueryExecutor() {
        persistItems(
                new QueryableItemEntity("charlie", "standard", 3),
                new QueryableItemEntity("alpha", "priority", 1),
                new QueryableItemEntity("bravo", "standard", 2)
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.all(),
                List.of(OrderBy.descending(SCORE), OrderBy.ascending(NAME))
        );

        KeysetResult<QueryableItemEntity> first = (KeysetResult<QueryableItemEntity>) executor().query(
                criteria,
                new KeysetWindow(2)
        );
        KeysetResult<QueryableItemEntity> second =
                (KeysetResult<QueryableItemEntity>) executor().query(
                        criteria,
                        new KeysetWindow(2, first.nextPosition())
                );

        assertEquals(List.of("charlie", "bravo"), names(first));
        assertEquals(List.of("alpha"), names(second));
        assertTrue(first.hasNext());
        assertFalse(second.hasNext());
        assertEquals(List.of(2, "bravo"), first.nextPosition().values());
    }

    @Test
    void ordersNullsConsistentlyWithTheInMemoryExecutor() {
        persistItems(new QueryableItemEntity("null2", "active", 1),
                new QueryableItemEntity("b", "active", 1, "b"),
                new QueryableItemEntity("null1", "active", 1),
                new QueryableItemEntity("a", "active", 1, "a"));
        QueryCriteria criteria = new QueryCriteria(QueryExpression.all(),
                List.of(OrderBy.ascending(OPTIONAL_VALUE), OrderBy.ascending(NAME)));

        assertEquals(List.of("a", "b", "null1", "null2"), names(executor().query(criteria, UnboundedWindow.getInstance())));
    }

    @Test
    void continuesKeysetsAcrossNullOrderValues() {
        persistItems(new QueryableItemEntity("null2", "active", 1),
                new QueryableItemEntity("b", "active", 1, "b"),
                new QueryableItemEntity("null1", "active", 1),
                new QueryableItemEntity("a", "active", 1, "a"));
        QueryCriteria criteria = new QueryCriteria(QueryExpression.all(),
                List.of(OrderBy.ascending(OPTIONAL_VALUE), OrderBy.ascending(NAME)));
        KeysetResult<QueryableItemEntity> first = (KeysetResult<QueryableItemEntity>) executor().query(criteria, new KeysetWindow(2));
        KeysetResult<QueryableItemEntity> second = (KeysetResult<QueryableItemEntity>) executor().query(criteria, new KeysetWindow(1, first.nextPosition()));
        KeysetResult<QueryableItemEntity> third = (KeysetResult<QueryableItemEntity>) executor().query(criteria, new KeysetWindow(1, second.nextPosition()));

        assertEquals(List.of("a", "b"), names(first));
        assertEquals(List.of("null1"), names(second));
        assertEquals(List.of("null2"), names(third));
        assertNull(third.nextPosition());
    }

    @Test
    void appliesEachNullPlacementToOrderingAndKeysetProjections() {
        persistItems(new QueryableItemEntity("null2", "active", 1),
                new QueryableItemEntity("b", "active", 1, "b"),
                new QueryableItemEntity("null1", "active", 1),
                new QueryableItemEntity("a", "active", 1, "a"));
        for (OrderBy.Direction direction : OrderBy.Direction.values()) {
            for (OrderBy.NullPlacement placement : OrderBy.NullPlacement.values()) {
                QueryCriteria criteria = new QueryCriteria(QueryExpression.all(),
                        List.of(new OrderBy<>(OPTIONAL_VALUE, direction, placement), OrderBy.ascending(NAME)));
                List<String> nonNull = direction == OrderBy.Direction.ASCENDING ? List.of("a", "b") : List.of("b", "a");
                List<String> expected = new ArrayList<>();
                if (placement == OrderBy.NullPlacement.FIRST) {
                    expected.addAll(List.of("null1", "null2"));
                }
                expected.addAll(nonNull);
                if (placement == OrderBy.NullPlacement.LAST) {
                    expected.addAll(List.of("null1", "null2"));
                }
                assertEquals(expected, names(executor().query(criteria, UnboundedWindow.getInstance())));
                QueryPlan<String> plan = QueryPlan.builder(QueryValue.field(NAME)).criteria(criteria).build();
                List<String> paged = new ArrayList<>();
                KeysetPosition position = null;
                for (int index = 0; index < expected.size(); index++) {
                    KeysetResult<String> page = (KeysetResult<String>) executor().query(plan, new KeysetWindow(1, position));
                    paged.addAll(page.getItems());
                    position = page.nextPosition();
                    if (index < expected.size() - 1) {
                        assertNotNull(position);
                    } else {
                        assertNull(position);
                    }
                }
                assertEquals(expected, paged);
            }
        }
    }

    @Test
    void executesTimeSeriesWindowThroughTheRegularJpaQueryExecutor() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        persistItems(
                new QueryableItemEntity("outside", "standard", 0, first.minusSeconds(1L)),
                new QueryableItemEntity("third", "standard", 3, first.plusSeconds(2L)),
                new QueryableItemEntity("first", "standard", 1, first),
                new QueryableItemEntity("second", "standard", 2, first.plusSeconds(1L))
        );

        QueryResult<QueryableItemEntity> queryResult = executor().query(
                QueryCriteria.empty(),
                new TimeSeriesWindow(first, first.plusSeconds(2L), 2)
        );

        assertTrue(queryResult instanceof SliceResult<?>);
        assertEquals(List.of("first", "second"), names(queryResult));
        assertTrue(((SliceResult<QueryableItemEntity>) queryResult).hasNext());
    }

    @Test
    void findsOneWithoutExecutingAnExactCountQuery() {
        persistItems(
                new QueryableItemEntity("alpha", "standard", 1),
                new QueryableItemEntity("bravo", "standard", 2)
        );

        Optional<QueryableItemEntity> item = executor().findOne(new QueryCriteria(
                QueryCondition.equal(NAME, "alpha"),
                List.of()
        ));

        assertEquals(Optional.of("alpha"), item.map(QueryableItemEntity::name));
        assertThrows(
                NonUniqueResultException.class,
                () -> executor().findOne(new QueryCriteria(
                        QueryCondition.equal(CATEGORY, "standard"),
                        List.of()
                ))
        );
    }

    @Test
    void supportsAnExplicitResolverForRenamedAttributes() {
        persistItems(new QueryableItemEntity("alice", "active", 10));
        QueryCriteria criteria = new QueryCriteria(
                QueryCondition.equal(DISPLAY_NAME, "alice"),
                List.of()
        );

        List<String> names = query(criteria, new ItemFieldResolver()).stream()
                .map(QueryableItemEntity::name)
                .toList();

        assertEquals(List.of("alice"), names);
    }

    @Test
    void appliesAConfiguredNamingStrategyToDefaultAttributeResolution() {
        persistItems(new QueryableItemEntity("alice", "active", 10));
        QueryCriteria criteria = new QueryCriteria(
                QueryCondition.equal(DISPLAY_NAME_SNAKE, "alice"),
                List.of()
        );

        List<String> names = new JpaQueryExecutor<>(
                entityManager,
                QueryableItemEntity.class,
                JpaFieldNamingStrategy.from(
                        CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL)
                )
        ).query(criteria, UnboundedWindow.getInstance()).getItems().stream()
                .map(QueryableItemEntity::name)
                .toList();

        assertEquals(List.of("alice"), names);
    }

    @Test
    void executesProjectionAggregateAndHavingPlansThroughJpa() {
        persistItems(
                new QueryableItemEntity("alpha", "standard", 10),
                new QueryableItemEntity("bravo", "standard", 20),
                new QueryableItemEntity("charlie", "priority", 30)
        );
        QueryPlan<GroupSummary> plan = QueryPlan.builder(
                        QueryProjection.of(
                                List.of(QueryValue.field(CATEGORY), QueryValue.count(), QueryValue.sum(QueryValue.field(SCORE))),
                                values -> new GroupSummary(
                                        (String) values.get(0),
                                        (Long) values.get(1),
                                        (BigDecimal) values.get(2)
                                )
                        )
                )
                .criteria(new QueryCriteria(QueryExpression.all(), List.of(OrderBy.ascending(CATEGORY))))
                .groupBy(QueryValue.field(CATEGORY))
                .having(QueryValueCondition.greaterThan(QueryValue.count(), 1L))
                .build();

        QueryResult<GroupSummary> result = executor().query(plan, UnboundedWindow.getInstance());

        assertEquals(List.of(new GroupSummary("standard", 2L, new BigDecimal("30"))), result.getItems());
    }

    @Test
    void executesDistinctAndKeysetProjectionPlansThroughJpa() {
        persistItems(
                new QueryableItemEntity("charlie", "standard", 3),
                new QueryableItemEntity("alpha", "priority", 1),
                new QueryableItemEntity("bravo", "standard", 2)
        );
        QueryPlan<String> distinctPlan = QueryPlan.builder(QueryValue.field(CATEGORY))
                .distinct()
                .build();
        QueryResult<String> distinct = executor().query(distinctPlan, UnboundedWindow.getInstance());

        QueryPlan<String> keysetPlan = QueryPlan.builder(QueryValue.field(NAME))
                .criteria(new QueryCriteria(
                        QueryExpression.all(),
                        List.of(OrderBy.ascending(SCORE), OrderBy.ascending(NAME))
                ))
                .build();
        KeysetResult<String> first = (KeysetResult<String>) executor().query(keysetPlan, new KeysetWindow(2));
        KeysetResult<String> second = (KeysetResult<String>) executor().query(
                keysetPlan,
                new KeysetWindow(2, first.nextPosition())
        );

        assertEquals(Set.of("standard", "priority"), Set.copyOf(distinct.getItems()));
        assertEquals(List.of("alpha", "bravo"), first.getItems());
        assertEquals(List.of("charlie"), second.getItems());
    }

    @Test
    void preservesNullableProjectionValuesAndSqlAggregateSemantics() {
        persistItems(new QueryableItemEntity("alpha", "active", 1, (String) null),
                new QueryableItemEntity("beta", "active", 2, "same"),
                new QueryableItemEntity("gamma", "active", 3, "same"));
        QueryPlan<List<Object>> aggregate = QueryPlan.builder(QueryProjection.of(List.of(
                QueryValue.count(QueryValue.field(OPTIONAL_VALUE)),
                QueryValue.countDistinct(QueryValue.field(OPTIONAL_VALUE))
        ), List::copyOf)).build();
        QueryPlan<String> projection = QueryPlan.builder(QueryProjection.of(List.of(QueryValue.field(OPTIONAL_VALUE)),
                        values -> values.get(0) == null ? "absent" : values.get(0).toString()))
                .criteria(new QueryCriteria(QueryExpression.all(), List.of(OrderBy.ascending(NAME))))
                .build();

        assertEquals(List.of(List.of(2L, 1L)), executor().query(aggregate, UnboundedWindow.getInstance()).getItems());
        assertEquals(List.of("absent", "same", "same"),
                executor().query(projection, UnboundedWindow.getInstance()).getItems());
        QueryPlan<Optional<String>> optional = QueryPlan.builder(
                        QueryProjection.optional(QueryValue.field(OPTIONAL_VALUE)))
                .criteria(new QueryCriteria(QueryExpression.all(), List.of(OrderBy.ascending(NAME))))
                .build();
        assertEquals(List.of(Optional.empty(), Optional.of("same"), Optional.of("same")),
                executor().query(optional, UnboundedWindow.getInstance()).getItems());
        QueryPlan<Optional<BigDecimal>> emptySum = QueryPlan.builder(
                        QueryProjection.optional(QueryValue.sum(QueryValue.field(SCORE))))
                .criteria(new QueryCriteria(QueryCondition.equal(NAME, "none"), List.of())).build();
        assertEquals(List.of(Optional.empty()), executor().query(emptySum, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void executesApplicationAggregatesWithTypedOperandsAndHaving() {
        persistItems(new QueryableItemEntity("alpha", "active", 2),
                new QueryableItemEntity("beta", "active", 3));
        QueryAggregateDefinition<Long> squares = QueryAggregateDefinition.of(
                "sumSquares", Long.class, Integer.class, Integer.class
        );
        QueryValue<Long> value = QueryValue.aggregate(squares, QueryValue.field(SCORE), QueryValue.field(SCORE));
        QueryPlan<Long> plan = QueryPlan.builder(value).having(QueryValueCondition.greaterThan(value, 10L)).build();
        QuerySchema.builder().add(SCORE, Set.of(QueryOperator.GREATER_THAN), false, Set.of(), Set.of(squares))
                .build().validate(plan, QueryComplexityBudget.defaults());
        JpaCriteriaTranslator translator = JpaCriteriaTranslator.builder().aggregate(squares,
                (builder, source, arguments) -> builder.sum(builder.prod(
                        arguments.get(0).as(Long.class), arguments.get(1).as(Long.class)
                ))).build();
        JpaQueryExecutor<QueryableItemEntity> custom = new JpaQueryExecutor<>(
                entityManager, QueryableItemEntity.class, translator, JpaQueryFieldResolver.byAttributeName()
        );

        assertEquals(List.of(13L), custom.query(plan, UnboundedWindow.getInstance()).getItems());
        assertThrows(IllegalArgumentException.class, () -> executor().query(plan, UnboundedWindow.getInstance()));
    }

    @Test
    void rejectsApplicationOwnedShapesWithoutACustomJpaExecutor() {
        assertThrows(IllegalArgumentException.class, () -> executor().query(QueryCriteria.empty(), new CustomWindow()));
        assertThrows(IllegalArgumentException.class,
                () -> executor().query(new CustomPlan(), UnboundedWindow.getInstance()));
    }

    @Test
    void rejectsApplicationNodesEvenWhenTheyDispatchAsBuiltInNodes() {
        QueryCriteria criteria = new QueryCriteria(new CustomExpression(), List.of());
        QueryPlan<Integer> plan = QueryPlan.builder(new CustomValue()).build();

        assertThrows(IllegalArgumentException.class, () -> executor().query(criteria, UnboundedWindow.getInstance()));
        assertThrows(IllegalArgumentException.class, () -> executor().query(plan, UnboundedWindow.getInstance()));
    }

    @Test
    void executesTimeSeriesAggregationAndResamplingThroughJpa() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        persistItems(
                new QueryableItemEntity("first", "standard", 10, first.plusSeconds(60L)),
                new QueryableItemEntity("second", "standard", 20, first.plusSeconds(120L)),
                new QueryableItemEntity("third", "standard", 30, first.plusSeconds(3_660L))
        );
        QueryPlan<TimeSeriesPoint<BigDecimal>> plan = QueryPlan.sum(
                        QueryValue.field(TimeSeriesPoint.TIMESTAMP),
                        QueryValue.field(SCORE),
                        TimeSeriesBucket.of(Duration.ofHours(1), first)
                )
                .build();

        QueryResult<TimeSeriesPoint<BigDecimal>> result = executor().query(
                plan,
                new TimeSeriesWindow(first, first.plusSeconds(7_200L), 10)
        );

        assertEquals(
                List.of(
                        new TimeSeriesPoint<>(first, new BigDecimal("30")),
                        new TimeSeriesPoint<>(first.plusSeconds(3_600L), new BigDecimal("30"))
                ),
                result.getItems()
        );
    }

    @Test
    void executesApplicationTimeSeriesStrategiesAgainstJpa() {
        Instant january = Instant.parse("2026-01-31T12:00:00Z");
        Instant february = Instant.parse("2026-02-02T12:00:00Z");
        persistItems(new QueryableItemEntity("alpha", "active", 2, january),
                new QueryableItemEntity("beta", "active", 3, february));
        TimeSeriesBucket months = timestamp -> timestamp.atZone(ZoneOffset.UTC).toLocalDate()
                .withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        TimeSeriesAggregationFunction squares = (values, hasValue) -> {
            long total = 0;
            for (Object value : values) {
                if (value != null) {
                    total += (long) (Integer) value * (Integer) value;
                }
            }
            return total;
        };
        QueryPlan<TimeSeriesPoint<Long>> plan = QueryPlan.timeSeries(
                Long.class, QueryValue.field(TimeSeriesPoint.TIMESTAMP), QueryValue.field(SCORE), squares, months
        ).build();
        QuerySchema.builder().add(TimeSeriesPoint.TIMESTAMP, Set.of(QueryOperator.EQUAL))
                .add(SCORE, Set.of(QueryOperator.EQUAL)).timeSeriesAggregation(squares, Set.of(SCORE))
                .build().validate(plan, QueryComplexityBudget.defaults());

        assertEquals(List.of(new TimeSeriesPoint<>(Instant.parse("2026-01-01T00:00:00Z"), 4L),
                        new TimeSeriesPoint<>(Instant.parse("2026-02-01T00:00:00Z"), 9L)),
                executor().query(plan, new TimeSeriesWindow(january, february, 10)).getItems());
    }

    @Test
    void reducesTimeSeriesValuesInTimestampOrder() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        persistItems(new QueryableItemEntity("late", "active", 9, start.plusSeconds(120)),
                new QueryableItemEntity("early", "active", 2, start.plusSeconds(60)));
        TimeSeriesAggregationFunction last = (values, hasValue) -> values.get(values.size() - 1);
        QueryPlan<TimeSeriesPoint<Integer>> plan = QueryPlan.timeSeries(
                Integer.class, QueryValue.field(TimeSeriesPoint.TIMESTAMP), QueryValue.field(SCORE), last,
                TimeSeriesBucket.of(Duration.ofHours(1), start)
        ).build();

        assertEquals(List.of(new TimeSeriesPoint<>(start, 9)),
                executor().query(plan, new TimeSeriesWindow(start, start.plusSeconds(3_600), 10)).getItems());
    }

    private List<QueryableItemEntity> query(QueryCriteria criteria) {
        return executor().query(criteria, UnboundedWindow.getInstance()).getItems();
    }

    private static final class CustomWindow implements QueryWindow {
    }

    private static final class CustomExpression implements QueryExpression {
        @Override
        public <R> R accept(QueryExpressionVisitor<R> visitor) {
            return visitor.visitMatchAll();
        }
    }

    private static final class CustomValue implements QueryValue<Integer> {
        @Override
        public Class<Integer> getValueType() {
            return Integer.class;
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return QueryValue.literal(1).accept(visitor);
        }
    }

    private static final class CustomPlan implements QueryPlan<String> {
        @Override
        public QueryCriteria getCriteria() {
            return QueryCriteria.empty();
        }
    }

    private List<QueryableItemEntity> query(
            QueryCriteria criteria,
            JpaQueryFieldResolver<QueryableItemEntity> fieldResolver
    ) {
        return new JpaQueryExecutor<>(entityManager, QueryableItemEntity.class, fieldResolver)
                .query(criteria, UnboundedWindow.getInstance())
                .getItems();
    }

    private JpaQueryExecutor<QueryableItemEntity> executor() {
        return new JpaQueryExecutorFactory(entityManager).forEntity(QueryableItemEntity.class);
    }

    private List<String> names(QueryResult<QueryableItemEntity> result) {
        return result.getItems().stream().map(QueryableItemEntity::name).toList();
    }

    private void persistItems(QueryableItemEntity... items) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (QueryableItemEntity item : items) {
                entityManager.persist(item);
            }
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }
        entityManager.clear();
    }

    private static final class GroupSummary {
        private final String category;
        private final long count;
        private final BigDecimal totalScore;

        private GroupSummary(String category, long count, BigDecimal totalScore) {
            this.category = category;
            this.count = count;
            this.totalScore = totalScore;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GroupSummary summary)) {
                return false;
            }
            return count == summary.count && category.equals(summary.category) && totalScore.equals(summary.totalScore);
        }

        @Override
        public int hashCode() {
            return Objects.hash(category, count, totalScore);
        }
    }

    private static final class ItemFieldResolver implements JpaQueryFieldResolver<QueryableItemEntity> {
        @Override
        @NonNull
        public <V> Expression<V> resolve(@NonNull Root<QueryableItemEntity> root, @NonNull QueryField<V> field) {
            Objects.requireNonNull(root, "root");
            if (DISPLAY_NAME.equals(field)) {
                return root.get("name");
            }
            if (NAME.equals(field)) {
                return root.get("name");
            }
            if (CATEGORY.equals(field)) {
                return root.get("category");
            }
            if (SCORE.equals(field)) {
                return root.get("score");
            }
            throw new IllegalArgumentException("Unsupported test query field: " + field.getName());
        }
    }
}
