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

package com.lamprism.luxspec.data.jpa.autoconfigure;

import com.lamprism.luxspec.data.jpa.JpaCriteriaTranslator;
import com.lamprism.luxspec.data.jpa.JpaFieldNamingStrategy;
import com.lamprism.luxspec.data.jpa.JpaQueryExecutorFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Supplies default JPA query translation and execution adapters.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass({JpaCriteriaTranslator.class, EntityManager.class})
public class LuxspecDataJpaAutoConfiguration {
    /**
     * Creates a shared typed Criteria translator when an application has not supplied one.
     *
     * @return the stateless Criteria translator
     */
    @Bean
    @ConditionalOnMissingBean(JpaCriteriaTranslator.class)
    public JpaCriteriaTranslator jpaCriteriaTranslator() {
        return new JpaCriteriaTranslator();
    }

    /**
     * Creates the identity field naming strategy when the application has not supplied one.
     *
     * @return the default query-to-attribute naming strategy
     */
    @Bean
    @ConditionalOnMissingBean(JpaFieldNamingStrategy.class)
    public JpaFieldNamingStrategy jpaFieldNamingStrategy() {
        return JpaFieldNamingStrategy.identity();
    }

    /**
     * Creates an entity-specific query executor factory when JPA exposes an entity manager bean.
     *
     * @param entityManager  the application-managed entity manager
     * @param translator     the shared stateless Criteria translator
     * @param namingStrategy the query-to-attribute naming strategy
     * @return the query executor factory
     */
    @Bean
    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean(JpaQueryExecutorFactory.class)
    public JpaQueryExecutorFactory jpaQueryExecutorFactory(
            EntityManager entityManager,
            JpaCriteriaTranslator translator,
            JpaFieldNamingStrategy namingStrategy
    ) {
        return new JpaQueryExecutorFactory(entityManager, translator, namingStrategy);
    }
}
