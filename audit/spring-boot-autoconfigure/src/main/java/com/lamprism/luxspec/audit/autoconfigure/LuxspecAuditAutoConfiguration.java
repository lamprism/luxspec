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

import com.lamprism.luxspec.audit.integration.ExecutionContextAuditMetadataProvider;
import com.lamprism.luxspec.audit.integration.StandardAuditEventCatalog;
import com.lamprism.luxspec.audit.publish.AuditDeliveryPolicy;
import com.lamprism.luxspec.audit.publish.AuditEventIdGenerator;
import com.lamprism.luxspec.audit.publish.AuditEventRegistry;
import com.lamprism.luxspec.audit.publish.AuditMetadataProvider;
import com.lamprism.luxspec.audit.publish.AuditPublicationErrorHandler;
import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.audit.publish.AuditSink;
import com.lamprism.luxspec.audit.publish.DefaultAuditPublisher;
import com.lamprism.luxspec.audit.publish.UuidAuditEventIdGenerator;
import com.lamprism.luxspec.audit.query.AuditReader;
import com.lamprism.luxspec.audit.store.InMemoryAuditStore;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

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
@AutoConfigureAfter(name = "com.lamprism.luxspec.core.autoconfigure.LuxspecCoreAutoConfiguration")
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
     * Creates the built-in translator registry when no registry is supplied.
     *
     * @return the standard Luxspec translator registry
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean(AuditRegistry.class)
    public AuditRegistry luxspecAuditRegistry() {
        return StandardAuditEventCatalog.createRegistry();
    }

    /**
     * Creates the execution-context metadata provider when no provider is supplied.
     *
     * @return the default audit metadata provider
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean(AuditMetadataProvider.class)
    public AuditMetadataProvider luxspecAuditMetadataProvider() {
        return new ExecutionContextAuditMetadataProvider();
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
     * Creates the required delivery policy when no policy is supplied.
     *
     * @return the default required delivery policy
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean(AuditDeliveryPolicy.class)
    public AuditDeliveryPolicy luxspecAuditDeliveryPolicy() {
        return AuditDeliveryPolicy.REQUIRED;
    }

    /**
     * Creates the publisher from application-owned sink and optional assembly roles.
     *
     * @param registry          the translator registry
     * @param sink              the application-owned sink
     * @param clocks            optional publication clocks
     * @param idGenerator       the event ID generator
     * @param metadataProviders optional metadata providers
     * @param policy            the delivery policy
     * @param errorHandlers     optional best-effort failure handlers
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
            AuditDeliveryPolicy policy,
            ObjectProvider<AuditPublicationErrorHandler> errorHandlers
    ) {
        Clock clock = clocks.getIfAvailable(Clock::systemUTC);
        AuditMetadataProvider metadataProvider = metadataProviders.getIfAvailable(ExecutionContextAuditMetadataProvider::new);
        AuditPublicationErrorHandler errorHandler = errorHandlers.getIfAvailable();
        return new DefaultAuditPublisher(
                registry,
                sink,
                clock,
                idGenerator,
                metadataProvider,
                policy,
                errorHandler
        );
    }

    /**
     * Registers the built-in configuration, security, and user lifecycle translations.
     *
     * @param dispatcher the event dispatcher
     * @param publisher  the audit publisher
     * @return the closeable standard event registry
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnBean({EventDispatcher.class, AuditPublisher.class})
    @ConditionalOnMissingBean(AuditEventRegistry.class)
    public AuditEventRegistry luxspecAuditEventRegistry(
            EventDispatcher dispatcher,
            AuditPublisher publisher
    ) {
        return StandardAuditEventCatalog.createEventRegistry(dispatcher, publisher);
    }
}
