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

package com.lamprism.luxspec.audit.publish;

import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditEntryContent;
import com.lamprism.luxspec.audit.AuditEnvelope;
import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditMetadata;
import com.lamprism.luxspec.audit.AuditNameValidator;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Synchronously translates and delivers audit envelopes through one sink.
 *
 * <p>The convenience publication method captures the event ID, publication
 * time, and metadata once. Publishing an existing envelope preserves those
 * values for retries and replays. Translation and sink failures follow the
 * configured delivery policy; no failure is silently discarded by the
 * required default policy.</p>
 *
 * @author RollW
 */
public class DefaultAuditPublisher implements AuditPublisher {
    private final AuditRegistry registry;
    private final AuditSink sink;
    private final Clock clock;
    private final AuditEventIdGenerator idGenerator;
    private final AuditMetadataProvider metadataProvider;
    private final AuditDeliveryPolicy deliveryPolicy;
    private final @Nullable AuditPublicationErrorHandler errorHandler;

    /**
     * Creates a publisher with UTC time, UUID event IDs, unknown actor
     * metadata, and required delivery.
     *
     * @param registry the immutable translator registry
     * @param sink     the synchronous sink
     */
    public DefaultAuditPublisher(AuditRegistry registry, AuditSink sink) {
        this(
                registry,
                sink,
                Clock.systemUTC(),
                new UuidAuditEventIdGenerator(),
                () -> new AuditMetadata(AuditActor.unknown(), null, AuditFieldSet.empty()),
                AuditDeliveryPolicy.REQUIRED,
                null
        );
    }

    /**
     * Creates a publisher with explicit publication and delivery policies.
     *
     * @param registry         the immutable translator registry
     * @param sink             the synchronous sink
     * @param clock            the publication clock
     * @param idGenerator      the local event ID generator
     * @param metadataProvider the publication metadata provider
     * @param deliveryPolicy   the translation and sink failure policy
     * @param errorHandler     the best-effort failure handler
     */
    public DefaultAuditPublisher(
            AuditRegistry registry,
            AuditSink sink,
            Clock clock,
            AuditEventIdGenerator idGenerator,
            AuditMetadataProvider metadataProvider,
            AuditDeliveryPolicy deliveryPolicy,
            @Nullable AuditPublicationErrorHandler errorHandler
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
        this.deliveryPolicy = Objects.requireNonNull(deliveryPolicy, "deliveryPolicy");
        if (deliveryPolicy == AuditDeliveryPolicy.BEST_EFFORT && errorHandler == null) {
            throw new IllegalArgumentException("BEST_EFFORT delivery requires an error handler");
        }
        this.errorHandler = errorHandler;
    }

    @Override
    public <E> void publish(String eventName, E event) {
        String normalizedName = AuditNameValidator.require(eventName);
        E nonNullEvent = Objects.requireNonNull(event, "event");
        AuditEventId id = Objects.requireNonNull(idGenerator.nextId(normalizedName), "generated audit event ID");
        Instant publishedAt = Objects.requireNonNull(clock.instant(), "clock instant");
        AuditMetadata metadata = Objects.requireNonNull(metadataProvider.capture(), "audit metadata");
        publish(new AuditEnvelope<>(id, normalizedName, publishedAt, metadata, nonNullEvent));
    }

    @Override
    public <E> void publish(AuditEnvelope<E> envelope) {
        AuditEnvelope<E> nonNullEnvelope = Objects.requireNonNull(envelope, "envelope");
        AuditEventTranslator<E> translator = translator(nonNullEnvelope.eventName());
        AuditEntryContent content;
        try {
            content = translator.translate(nonNullEnvelope);
        } catch (Throwable failure) {
            handleFailure(nonNullEnvelope, null, failure);
            return;
        }
        if (content == null) {
            return;
        }
        AuditEntry entry = new AuditEntry(
                nonNullEnvelope.id(),
                nonNullEnvelope.eventName(),
                content.occurredAt(),
                nonNullEnvelope.publishedAt(),
                nonNullEnvelope.metadata(),
                content.action(),
                content.outcome(),
                content.resource(),
                content.fieldSet(),
                content.detail()
        );
        try {
            sink.accept(entry);
        } catch (Throwable failure) {
            handleFailure(nonNullEnvelope, entry, failure);
        }
    }

    @SuppressWarnings("unchecked")
    private <E> AuditEventTranslator<E> translator(String eventName) {
        AuditEventTranslator<?> translator = registry.find(eventName);
        if (translator == null) {
            throw new AuditConfigurationException("No audit translator is registered for: " + eventName);
        }
        return (AuditEventTranslator<E>) translator;
    }

    private void handleFailure(AuditEnvelope<?> envelope, @Nullable AuditEntry entry, Throwable failure) {
        if (deliveryPolicy == AuditDeliveryPolicy.REQUIRED) {
            throw new AuditPublicationException(envelope, failure);
        }
        errorHandler.onFailure(envelope, entry, failure);
    }
}
