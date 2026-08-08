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
import com.lamprism.luxspec.config.persistence.JpaConfigEntry;
import com.lamprism.luxspec.config.persistence.JpaConfigEntryId;
import com.lamprism.luxspec.config.persistence.JpaConfigEntryRepository;
import com.lamprism.luxspec.config.persistence.JpaConfigSource;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecConfigJpaAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecConfigJpaAutoConfiguration.class));

    @Test
    void waitsForApplicationRepository() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(JpaConfigSource.class));
    }

    @Test
    void createsSourceFromApplicationRepository() {
        contextRunner
                .withBean(JpaConfigEntryRepository.class, TestRepository::new)
                .withPropertyValues("luxspec.config.jpa.source-id=primary")
                .run(context -> {
                    assertThat(context).hasSingleBean(JpaConfigSource.class);
                    assertThat(context.getBean(JpaConfigSource.class).getId().getValue())
                            .isEqualTo("primary");
                });
    }

    @Test
    void createsDefaultRepositoryFromEntityManager() {
        contextRunner
                .withBean(EntityManager.class, LuxspecConfigJpaAutoConfigurationTest::entityManagerProxy)
                .run(context -> assertThat(context)
                        .hasSingleBean(EntityManagerJpaConfigEntryRepository.class)
                        .hasSingleBean(JpaConfigSource.class));
    }

    private static EntityManager entityManagerProxy() {
        return (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) {
                        return arguments != null && arguments.length == 1 && proxy == arguments[0];
                    }
                    if (method.getName().equals("toString")) {
                        return "test-entity-manager";
                    }
                    return null;
                }
        );
    }

    private static final class TestRepository implements JpaConfigEntryRepository {
        @Override
        public Optional<JpaConfigEntry> findById(JpaConfigEntryId id) {
            return Optional.empty();
        }

        @Override
        public JpaConfigEntry save(JpaConfigEntry entry) {
            return entry;
        }

        @Override
        public void deleteById(JpaConfigEntryId id) {
        }
    }
}
