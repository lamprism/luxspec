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

package com.lamprism.luxspec.config.persistence.autoconfigure;

import com.lamprism.luxspec.config.persistence.EntityManagerJpaConfigEntryRepository;
import com.lamprism.luxspec.config.persistence.JpaConfigEntryRepository;
import com.lamprism.luxspec.config.persistence.JpaConfigSource;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Registers the optional JPA Config source over an application-managed JPA boundary.
 *
 * <p>This configuration does not scan entities, configure DDL, or start migrations. The
 * application remains responsible for persistence discovery and schema management.</p>
 *
 * @author RollW
 */
@AutoConfiguration(beforeName = "com.lamprism.luxspec.config.autoconfigure.LuxspecConfigAutoConfiguration")
@ConditionalOnClass({JpaConfigSource.class, JpaConfigEntryRepository.class, EntityManager.class})
@EnableConfigurationProperties(LuxspecConfigJpaConfiguration.class)
public class LuxspecConfigJpaAutoConfiguration {
    /**
     * Creates the default Config entry repository over the application entity manager.
     *
     * @param entityManager the application-managed entity manager
     * @return the default entry repository
     */
    @Bean
    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean(JpaConfigEntryRepository.class)
    public JpaConfigEntryRepository configEntryRepository(EntityManager entityManager) {
        return new EntityManagerJpaConfigEntryRepository(entityManager);
    }

    /**
     * Creates the database-backed Config source when one has not been supplied.
     *
     * @param repository the application-created Spring Data repository
     * @param properties the source settings
     * @param clocks     the optional application clock
     * @return the JPA configuration source
     */
    @Bean
    @ConditionalOnBean(JpaConfigEntryRepository.class)
    @ConditionalOnMissingBean(JpaConfigSource.class)
    public JpaConfigSource configSource(
            JpaConfigEntryRepository repository,
            LuxspecConfigJpaConfiguration properties,
            ObjectProvider<Clock> clocks
    ) {
        Clock clock = clocks.getIfAvailable(Clock::systemUTC);
        return new JpaConfigSource(properties.toSourceId(), repository, clock);
    }
}
