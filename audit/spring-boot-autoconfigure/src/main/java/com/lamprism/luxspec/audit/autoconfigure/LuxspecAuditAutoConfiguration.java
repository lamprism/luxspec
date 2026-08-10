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
import com.lamprism.luxspec.audit.integration.StandardAuditEventRegistration;
import com.lamprism.luxspec.audit.integration.StandardAuditRegistry;
import com.lamprism.luxspec.audit.publish.AuditDeliveryPolicy;
import com.lamprism.luxspec.audit.publish.AuditEventIdGenerator;
import com.lamprism.luxspec.audit.publish.AuditMetadataProvider;
import com.lamprism.luxspec.audit.publish.AuditPublicationErrorHandler;
import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.audit.publish.AuditSink;
import com.lamprism.luxspec.audit.publish.DefaultAuditPublisher;
import com.lamprism.luxspec.audit.publish.UuidAuditEventIdGenerator;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Assembles the standard Luxspec audit event integration when the application supplies an audit
 * sink. The sink remains application-owned, and an application-provided publisher or registry
 * takes precedence over the defaults. When an application supplies an {@link EventDispatcher},
 * that dispatcher is reused. A custom {@link EventPublisher} that is not an event dispatcher is
 * also respected; in that case the application must assemble the standard registration against
 * its own dispatching path because an opaque publisher cannot expose subscriptions.
 *
 * @author RollW
 */
@AutoConfiguration
public class LuxspecAuditAutoConfiguration {
    /**
     * Creates the built-in translator registry when no registry is supplied.
     *
     * @return the standard Luxspec event registry
     */
    @Bean
    @ConditionalOnBean(AuditSink.class)
    @ConditionalOnMissingBean(AuditRegistry.class)
    public AuditRegistry luxspecAuditRegistry() {
        return StandardAuditRegistry.create();
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
     * Creates the publisher from application-owned sink and optional assembly roles.
     *
     * @param registry          the translator registry
     * @param sink              the application-owned sink
     * @param clocks            optional publication clocks
     * @param idGenerators      optional event ID generators
     * @param metadataProviders optional metadata providers
     * @param policies          optional delivery policies
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
            ObjectProvider<AuditEventIdGenerator> idGenerators,
            ObjectProvider<AuditMetadataProvider> metadataProviders,
            ObjectProvider<AuditDeliveryPolicy> policies,
            ObjectProvider<AuditPublicationErrorHandler> errorHandlers
    ) {
        Clock clock = clocks.getIfAvailable(Clock::systemUTC);
        AuditEventIdGenerator idGenerator = idGenerators.getIfAvailable(UuidAuditEventIdGenerator::new);
        AuditMetadataProvider metadataProvider = metadataProviders.getIfAvailable(ExecutionContextAuditMetadataProvider::new);
        AuditDeliveryPolicy policy = policies.getIfAvailable(() -> AuditDeliveryPolicy.REQUIRED);
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
     * Creates a synchronous event dispatcher when the audit publisher is present and no event
     * publisher has been assembled by the application.
     *
     * <p>The default failure handler rethrows listener failures so required audit delivery is not
     * silently converted into a successful event publication.</p>
     *
     * @return the event dispatcher used by the standard audit registration
     */
    @Bean
    @ConditionalOnBean(AuditPublisher.class)
    @ConditionalOnMissingBean({EventDispatcher.class, EventPublisher.class})
    public EventDispatcher luxspecAuditEventDispatcher() {
        return new EventDispatcher((event, listener, failure) -> {
            throw new IllegalStateException(
                    "Event listener failed for: " + event.getClass().getName(),
                    failure
            );
        });
    }

    /**
     * Registers the built-in configuration, security, and user lifecycle translations.
     *
     * @param dispatcher the event dispatcher
     * @param publisher  the audit publisher
     * @return the closeable standard audit registration
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnBean({EventDispatcher.class, AuditPublisher.class})
    @ConditionalOnMissingBean(StandardAuditEventRegistration.class)
    public StandardAuditEventRegistration luxspecAuditEventRegistration(
            EventDispatcher dispatcher,
            AuditPublisher publisher
    ) {
        return new StandardAuditEventRegistration(dispatcher, publisher);
    }
}
