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

package com.lamprism.luxspec.audit.autoconfigure;

import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.integration.security.SecurityAuditEventNames;
import com.lamprism.luxspec.audit.publish.AuditDeliveryPolicy;
import com.lamprism.luxspec.audit.publish.AuditEventDefinition;
import com.lamprism.luxspec.audit.publish.AuditEventDefinitionContributor;
import com.lamprism.luxspec.audit.publish.AuditEventIdGenerator;
import com.lamprism.luxspec.audit.publish.AuditMetadataProvider;
import com.lamprism.luxspec.audit.publish.AuditPublicationFailure;
import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.audit.publish.AuditSink;
import com.lamprism.luxspec.audit.store.InMemoryAuditStore;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.core.autoconfigure.LuxspecEventAutoConfiguration;
import com.lamprism.luxspec.event.EventDispatchFailure;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventSubscription;
import com.lamprism.luxspec.event.SynchronousEventDispatcher;
import com.lamprism.luxspec.failure.FailureHandler;
import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.firewall.FirewallRuleFailureEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecAuditAutoConfigurationTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LuxspecEventAutoConfiguration.class,
                    LuxspecAuditAutoConfiguration.class
            ));

    @Test
    void createsThePublisherAndSubscribesBuiltInEventsWhenAnApplicationSuppliesASink() {
        List<AuditEntry> entries = new ArrayList<>();

        contextRunner
                .withBean(AuditSink.class, () -> entries::add)
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditRegistry.class);
                    assertThat(context).hasSingleBean(AuditPublisher.class);
                    assertThat(context).hasSingleBean(AuditEventIdGenerator.class);
                    assertThat(context).doesNotHaveBean(AuditMetadataProvider.class);
                    assertThat(context).doesNotHaveBean(AuditDeliveryPolicy.class);
                    assertThat(context).hasSingleBean(EventDispatcher.class);
                    assertThat(context.getBean(EventDispatcher.class))
                            .isInstanceOf(SynchronousEventDispatcher.class);
                    assertThat(context).hasSingleBean(EventSubscription.class);
                    assertThat(context.getBean(EventSubscription.class).isActive()).isTrue();

                    context.getBean(EventDispatcher.class)
                            .publish(new FirewallRuleFailureEvent(
                                    "example.FirewallRule",
                                    SecurityErrorCode.FIREWALL_RULE_FAILURE,
                                    OCCURRED_AT
                            ));

                    assertThat(entries).hasSize(1);
                    assertThat(entries.get(0).eventName())
                            .isEqualTo(SecurityAuditEventNames.FIREWALL_RULE_FAILED);
                    assertThat(entries.get(0).outcome()).isEqualTo(AuditOutcome.FAILURE);
                });
    }

    @Test
    void doesNotCreateAuditRuntimeWithoutAnApplicationSink() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AuditPublisher.class);
            assertThat(context).doesNotHaveBean(AuditRegistry.class);
            assertThat(context).hasSingleBean(EventDispatcher.class);
            assertThat(context).doesNotHaveBean(EventSubscription.class);
        });
    }

    @Test
    void createsTheOptInSharedInMemoryAuditStore() {
        contextRunner
                .withPropertyValues("luxspec.audit.memory.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(InMemoryAuditStore.class);
                    assertThat(context).hasSingleBean(AuditSink.class);
                    assertThat(context).hasSingleBean(com.lamprism.luxspec.audit.query.AuditReader.class);
                    assertThat(context).hasSingleBean(AuditPublisher.class);
                });
    }

    @Test
    void createsContextMetadataProviderOnlyWhenContextStorageIsSupplied() {
        contextRunner
                .withBean(ExecutionContextStorage.class, ThreadLocalExecutionContextStorage::new)
                .withBean(AuditSink.class, () -> entry -> {
                })
                .run(context -> assertThat(context).hasSingleBean(AuditMetadataProvider.class));
    }

    @Test
    void includesApplicationProvidedEventDefinitionContributors() {
        List<AuditEntry> entries = new ArrayList<>();
        contextRunner
                .withBean(AuditEventDefinitionContributor.class, () -> registrar -> registrar.register(
                        AuditEventDefinition.of(
                                "application.test",
                                FirewallRuleFailureEvent.class,
                                envelope -> null
                        )
                ))
                .withBean(AuditSink.class, () -> entries::add)
                .run(context -> assertThat(context.getBean(AuditRegistry.class).find("application.test"))
                        .isNotNull());
    }

    @Test
    void doesNotCreateBuiltInContributorsWhenFeatureEventTypesAreAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(
                        "com.lamprism.luxspec.config.event",
                        "com.lamprism.luxspec.security.authentication",
                        "com.lamprism.luxspec.user.lifecycle"
                ))
                .withBean(AuditSink.class, () -> entry -> {
                })
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditEventDefinitionContributor.class);
                    assertThat(context).hasSingleBean(AuditRegistry.class);
                });
    }

    @Test
    void selectsTheAuditFailureHandlerByItsGenericContext() {
        contextRunner
                .withUserConfiguration(GenericFailureHandlers.class)
                .withBean(AuditSink.class, () -> entry -> {
                })
                .run(context -> assertThat(context).hasSingleBean(AuditPublisher.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class GenericFailureHandlers {
        @Bean
        FailureHandler<AuditPublicationFailure> auditFailureHandler() {
            return (context, failure) -> {
            };
        }

        @Bean
        FailureHandler<EventDispatchFailure> eventFailureHandler() {
            return (context, failure) -> {
            };
        }
    }
}
