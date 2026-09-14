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

import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditMetadata;
import com.lamprism.luxspec.audit.integration.ExecutionContextAuditMetadataProvider;
import com.lamprism.luxspec.audit.integration.config.ConfigAuditEventDefinitionContributor;
import com.lamprism.luxspec.audit.integration.security.SecurityAuditEventDefinitionContributor;
import com.lamprism.luxspec.audit.integration.user.UserAuditEventDefinitionContributor;
import com.lamprism.luxspec.audit.publish.AuditDeliveryPolicy;
import com.lamprism.luxspec.audit.publish.AuditEventDefinitionContributor;
import com.lamprism.luxspec.audit.publish.AuditEventIdGenerator;
import com.lamprism.luxspec.audit.publish.AuditMetadataProvider;
import com.lamprism.luxspec.audit.publish.AuditPublicationFailure;
import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.audit.publish.AuditSink;
import com.lamprism.luxspec.audit.publish.DefaultAuditPublisher;
import com.lamprism.luxspec.audit.publish.UuidAuditEventIdGenerator;
import com.lamprism.luxspec.audit.query.AuditReader;
import com.lamprism.luxspec.audit.store.InMemoryAuditStore;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.event.EventSubscription;
import com.lamprism.luxspec.failure.FailureHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Assembles the standard Luxspec audit event integration when the application supplies an audit
 * sink. The sink remains application-owned, and an application-provided publisher or registry
 * takes precedence over the defaults. When an application supplies an {@link EventDispatcher},
 * that dispatcher is reused. A custom {@link EventPublisher} that is not an event dispatcher is
 * also respected; in that case the application must assemble the standard event registry against
 * its own dispatching path because an opaque publisher cannot expose subscriptions.
 *
 * @author RollW
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.lamprism.luxspec.core.autoconfigure.LuxspecEventAutoConfiguration",
        "com.lamprism.luxspec.core.autoconfigure.LuxspecExecutionContextStorageAutoConfiguration"
})
public class LuxspecAuditAutoConfiguration {
    /**
     * Creates the opt-in process-local audit sink and reader as one shared store.
     *
     * <p>No audit sink is created by default. Applications must explicitly choose this volatile
     * store or provide a durable sink so audit delivery cannot disappear silently.</p>
     *
     * @return the shared in-memory audit store
     */
    @Bean
    @ConditionalOnProperty(prefix = "luxspec.audit.memory", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean({AuditSink.class, AuditReader.class})
    public InMemoryAuditStore inMemoryAuditStore() {
        return new InMemoryAuditStore();
    }

    /**
     * Creates the built-in translator registry from the available event-definition contributors.
     *
     * @param contributors event-definition contributors
     * @return the standard Luxspec translator registry
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean(AuditRegistry.class)
    public AuditRegistry luxspecAuditRegistry(
            ObjectProvider<AuditEventDefinitionContributor> contributors
    ) {
        AuditRegistry.Builder registry = AuditRegistry.builder();
        contributors.orderedStream().forEach(contributor -> contributor.contribute(definition -> {
            Objects.requireNonNull(definition, "definition").register(registry);
        }));
        return registry.build();
    }

    /**
     * Creates the configuration event-definition contributor.
     *
     * @return the configuration event-definition contributor
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnClass(name = "com.lamprism.luxspec.config.event.ConfigSourceChangedEvent")
    public AuditEventDefinitionContributor luxspecConfigAuditEventDefinitionContributor() {
        return new ConfigAuditEventDefinitionContributor();
    }

    /**
     * Creates the security event-definition contributor.
     *
     * @return the security event-definition contributor
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnClass(name = "com.lamprism.luxspec.security.authentication.AuthenticationEvent")
    public AuditEventDefinitionContributor luxspecSecurityAuditEventDefinitionContributor() {
        return new SecurityAuditEventDefinitionContributor();
    }

    /**
     * Creates the user lifecycle event-definition contributor.
     *
     * @return the user lifecycle event-definition contributor
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnClass(name = "com.lamprism.luxspec.user.lifecycle.UserRegisteredEvent")
    public AuditEventDefinitionContributor luxspecUserAuditEventDefinitionContributor() {
        return new UserAuditEventDefinitionContributor();
    }

    /**
     * Creates the context-backed metadata provider only when the application explicitly supplies
     * context storage.
     *
     * @param storage the configured context storage
     * @return the context-backed metadata provider
     */
    @Bean
    @ConditionalOnBean({AuditSink.class, ExecutionContextStorage.class})
    @ConditionalOnMissingBean(AuditMetadataProvider.class)
    public AuditMetadataProvider luxspecAuditMetadataProvider(ExecutionContextStorage storage) {
        return new ExecutionContextAuditMetadataProvider(storage);
    }

    /**
     * Creates the default UUID event ID generator when no generator is supplied.
     *
     * @return the default event ID generator
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean(AuditEventIdGenerator.class)
    public AuditEventIdGenerator luxspecAuditEventIdGenerator() {
        return new UuidAuditEventIdGenerator();
    }

    /**
     * Creates the publisher from application-owned sink and optional assembly roles.
     *
     * @param registry          the translator registry
     * @param sink              the application-owned sink
     * @param clocks            optional publication clocks
     * @param idGenerator       the event ID generator
     * @param metadataProviders optional metadata providers
     * @param failureHandlers   optional best-effort failure handlers
     * @return the assembled audit publisher
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean(AuditPublisher.class)
    public AuditPublisher luxspecAuditPublisher(
            AuditRegistry registry,
            AuditSink sink,
            ObjectProvider<Clock> clocks,
            AuditEventIdGenerator idGenerator,
            ObjectProvider<AuditMetadataProvider> metadataProviders,
            ObjectProvider<FailureHandler<AuditPublicationFailure>> failureHandlers
    ) {
        Clock clock = clocks.getIfAvailable(Clock::systemUTC);
        AuditMetadataProvider metadataProvider = metadataProviders.getIfAvailable(
                () -> () -> new AuditMetadata(
                        AuditActor.unknown(),
                        null,
                        AuditFieldSet.empty()
                )
        );
        FailureHandler<AuditPublicationFailure> failureHandler = failureHandlers.getIfAvailable();
        return new DefaultAuditPublisher(
                registry,
                sink,
                clock,
                idGenerator,
                metadataProvider,
                AuditDeliveryPolicy.REQUIRED,
                failureHandler
        );
    }

    /**
     * Subscribes all event-definition contributors to the application dispatcher.
     *
     * @param dispatcher   the event dispatcher
     * @param publisher    the audit publisher
     * @param contributors event-definition contributors
     * @return the combined subscription for all contributed definitions
     */
    @Bean(destroyMethod = "unsubscribe")
    @ConditionalOnBean({EventDispatcher.class, AuditPublisher.class})
    @ConditionalOnMissingBean(EventSubscription.class)
    public EventSubscription luxspecAuditEventSubscriptions(
            EventDispatcher dispatcher,
            AuditPublisher publisher,
            ObjectProvider<AuditEventDefinitionContributor> contributors
    ) {
        List<EventSubscription> subscriptions = new ArrayList<>();
        try {
            contributors.orderedStream().forEach(contributor -> {
                Objects.requireNonNull(contributor, "contributor").contribute(definition -> {
                    EventSubscription subscription = Objects.requireNonNull(definition, "definition")
                            .subscribe(dispatcher, publisher);
                    subscriptions.add(subscription);
                });
            });
        } catch (RuntimeException failure) {
            try {
                EventSubscription.combine(subscriptions).unsubscribe();
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        return EventSubscription.combine(subscriptions);
    }
}
