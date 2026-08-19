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
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Executes typed Luxspec criteria through a JPA entity manager.
 *
 * <p>The default constructor path maps query field names to direct entity attributes with the same
 * name. Applications remain responsible for validating criteria with their query schema and for
 * managing the entity manager transaction boundary.
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
        throw new IllegalArgumentException("Unsupported query window: " + nonNullWindow.getClass().getName());
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
        boolean hasNext = fetchedItems.size() > window.limit();
        List<T> items = hasNext
                ? new ArrayList<>(fetchedItems.subList(0, window.limit()))
                : fetchedItems;
        return new SliceResult<>(items, window.offset(), window.limit(), hasNext);
    }

    private List<T> execute(QueryCriteria criteria, int offset, @Nullable Integer maxResults) {
        TypedQuery<T> query = createSelectionQuery(criteria);
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }

    private TypedQuery<T> createSelectionQuery(QueryCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityType);
        Root<T> root = query.from(entityType);
        query.select(root);
        Predicate predicate = translator.predicate(builder, root, criteria, fieldResolver);
        query.where(predicate);
        List<Order> orders = translator.orders(builder, root, criteria, fieldResolver);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        return entityManager.createQuery(query);
    }

    private long count(QueryCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<T> root = query.from(entityType);
        query.select(builder.count(root));
        query.where(translator.predicate(builder, root, criteria, fieldResolver));
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
}
