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

import com.lamprism.luxspec.data.pagination.KeysetResult;
import com.lamprism.luxspec.data.pagination.KeysetWindow;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.TimeSeriesWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.LikeCondition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryPlan;
import com.lamprism.luxspec.data.query.QueryProjection;
import com.lamprism.luxspec.data.query.QueryValue;
import com.lamprism.luxspec.data.query.QueryValueCondition;
import com.lamprism.luxspec.data.query.TimeSeriesBucket;
import com.lamprism.luxspec.data.query.TimeSeriesPoint;
import com.lamprism.luxspec.naming.CaseFormat;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaCriteriaTranslatorIntegrationTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<String> CATEGORY = QueryField.of("category", String.class);
    private static final QueryField<Integer> SCORE = QueryField.of("score", Integer.class);
    private static final QueryField<String> DISPLAY_NAME = QueryField.of("displayName", String.class);
    private static final QueryField<String> DISPLAY_NAME_SNAKE = QueryField.of("display_name", String.class);

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

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

    private List<QueryableItemEntity> query(QueryCriteria criteria) {
        return executor().query(criteria, UnboundedWindow.getInstance()).getItems();
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

    private record GroupSummary(String category, long count, BigDecimal totalScore) {
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
