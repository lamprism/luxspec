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

import jakarta.persistence.EntityManager;

import java.util.Objects;

/**
 * Creates entity-specific JPA query executors over one application-managed entity manager.
 *
 * @author RollW
 */
public class JpaQueryExecutorFactory {
    private final EntityManager entityManager;
    private final JpaCriteriaTranslator translator;
    private final JpaFieldNamingStrategy namingStrategy;

    /**
     * Creates a factory using the default stateless Criteria translator.
     *
     * @param entityManager the entity manager used by created executors
     */
    public JpaQueryExecutorFactory(EntityManager entityManager) {
        this(entityManager, new JpaCriteriaTranslator(), JpaFieldNamingStrategy.identity());
    }

    /**
     * Creates a factory using the supplied stateless Criteria translator.
     *
     * @param entityManager the entity manager used by created executors
     * @param translator    the Criteria translator shared by created executors
     */
    public JpaQueryExecutorFactory(EntityManager entityManager, JpaCriteriaTranslator translator) {
        this(entityManager, translator, JpaFieldNamingStrategy.identity());
    }

    /**
     * Creates a factory using the default Criteria translator and a field naming strategy.
     *
     * @param entityManager  the entity manager used by created executors
     * @param namingStrategy the query-to-attribute naming strategy
     */
    public JpaQueryExecutorFactory(EntityManager entityManager, JpaFieldNamingStrategy namingStrategy) {
        this(entityManager, new JpaCriteriaTranslator(), namingStrategy);
    }

    /**
     * Creates a factory using the supplied translator and field naming strategy.
     *
     * @param entityManager  the entity manager used by created executors
     * @param translator     the Criteria translator shared by created executors
     * @param namingStrategy the query-to-attribute naming strategy
     */
    public JpaQueryExecutorFactory(
            EntityManager entityManager,
            JpaCriteriaTranslator translator,
            JpaFieldNamingStrategy namingStrategy
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.translator = Objects.requireNonNull(translator, "translator");
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "namingStrategy");
    }

    /**
     * Creates an executor with the factory's configured direct attribute mapping.
     *
     * @param entityType the managed entity type
     * @param <T>        the persistence entity type
     * @return an entity-specific query executor
     */
    public <T> JpaQueryExecutor<T> forEntity(Class<T> entityType) {
        return forEntity(entityType, namingStrategy);
    }

    /**
     * Creates an executor with an entity-specific direct attribute naming strategy.
     *
     * @param entityType           the managed entity type
     * @param entityNamingStrategy the query-to-attribute naming strategy
     * @param <T>                  the persistence entity type
     * @return an entity-specific query executor
     */
    public <T> JpaQueryExecutor<T> forEntity(
            Class<T> entityType,
            JpaFieldNamingStrategy entityNamingStrategy
    ) {
        return new JpaQueryExecutor<>(
                entityManager,
                entityType,
                translator,
                JpaQueryFieldResolver.byAttributeName(entityNamingStrategy)
        );
    }

    /**
     * Creates an executor with an explicit field resolver.
     *
     * <p>Use this overload only when query fields do not map through the configured naming
     * strategy. The regular {@link #forEntity(Class)} path assembles the default resolver
     * internally.</p>
     *
     * @param entityType    the managed entity type
     * @param fieldResolver the field-to-attribute resolver
     * @param <T>           the persistence entity type
     * @return an entity-specific query executor
     */
    public <T> JpaQueryExecutor<T> forEntity(
            Class<T> entityType,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        return new JpaQueryExecutor<>(entityManager, entityType, translator, fieldResolver);
    }
}
