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

import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.LikeCondition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpressions;
import com.lamprism.luxspec.data.query.QueryField;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                QueryExpressions.and(List.of(
                        LikeCondition.of(NAME, "a%"),
                        ComparisonCondition.greaterThanOrEqualTo(SCORE, 10),
                        QueryExpressions.not(QueryCondition.equal(CATEGORY, "blocked"))
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
                QueryExpressions.or(List.of(
                        QueryCondition.in(CATEGORY, List.of("priority", "standard")),
                        QueryCondition.notIn(NAME, List.of("alpha", "charlie", "delta"))
                )),
                List.of(OrderBy.ascending(NAME))
        );

        List<String> names = query(criteria).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of("alpha", "bravo", "charlie"), names);
    }

    @Test
    void executesPageAndSliceWithoutManualCriteriaSetup() {
        persistItems(
                new QueryableItemEntity("charlie", "standard", 3),
                new QueryableItemEntity("alpha", "priority", 1),
                new QueryableItemEntity("bravo", "standard", 2)
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpressions.all(),
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
