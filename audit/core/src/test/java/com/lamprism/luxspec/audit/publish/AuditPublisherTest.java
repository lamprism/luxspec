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

import com.lamprism.luxspec.audit.AuditAction;
import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditEntryContent;
import com.lamprism.luxspec.audit.AuditEnvelope;
import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditMetadata;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditPublisherTest {
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant OCCURRED_AT = PUBLISHED_AT.minusSeconds(30L);
    private static final AuditEventId EVENT_ID = AuditEventId.of("event-1");
    private static final AuditMetadata METADATA = new AuditMetadata(
            AuditActor.of(AuditActor.Kind.USER, "user-1"),
            CorrelationId.of("request-1"),
            AuditFieldSet.empty()
    );

    @Test
    void capturesPublicationMetadataOnceAndPreservesEnvelopeOnReplay() {
        List<AuditEntry> entries = new ArrayList<>();
        AtomicInteger metadataCalls = new AtomicInteger();
        AuditRegistry registry = AuditRegistry.builder()
                .register("order.created", envelope -> AuditEntryContent.of(
                        OCCURRED_AT,
                        AuditAction.of("order.create"),
                        AuditOutcome.SUCCESS,
                        null,
                        AuditFieldSet.empty()
                ))
                .build();
        AuditPublisher publisher = new DefaultAuditPublisher(
                registry,
                entries::add,
                Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC),
                eventName -> EVENT_ID,
                () -> {
                    metadataCalls.incrementAndGet();
                    return METADATA;
                },
                AuditDeliveryPolicy.REQUIRED,
                null
        );

        publisher.publish(" order.created ", "payload");
        AuditEnvelope<String> replay = new AuditEnvelope<>(
                EVENT_ID,
                "order.created",
                PUBLISHED_AT,
                METADATA,
                "payload"
        );
        publisher.publish(replay);

        assertEquals(1, metadataCalls.get());
        assertEquals(2, entries.size());
        assertEquals(EVENT_ID, entries.get(0).id());
        assertEquals(OCCURRED_AT, entries.get(0).occurredAt());
        assertEquals(PUBLISHED_AT, entries.get(0).publishedAt());
        assertEquals(OCCURRED_AT, entries.get(1).occurredAt());
        assertEquals(PUBLISHED_AT, entries.get(1).publishedAt());
        assertSame(METADATA, entries.get(0).metadata());
        assertSame(METADATA, entries.get(1).metadata());
    }

    @Test
    void missingTranslatorIsConfigurationFailureEvenForBestEffort() {
        AuditPublisher publisher = new DefaultAuditPublisher(
                AuditRegistry.builder().build(),
                entry -> {
                },
                Clock.systemUTC(),
                eventName -> EVENT_ID,
                () -> METADATA,
                AuditDeliveryPolicy.BEST_EFFORT,
                (envelope, entry, failure) -> {
                }
        );

        assertThrows(AuditConfigurationException.class, () -> publisher.publish("unknown", "payload"));
    }

    @Test
    void requiredAndBestEffortPoliciesHandleSinkFailuresDifferently() {
        AuditRegistry registry = AuditRegistry.builder()
                .register("order.created", envelope -> AuditEntryContent.of(
                        OCCURRED_AT,
                        AuditAction.of("order.create"),
                        AuditOutcome.SUCCESS,
                        null,
                        AuditFieldSet.empty()
                ))
                .build();
        AuditEnvelope<String> envelope = new AuditEnvelope<>(
                EVENT_ID,
                "order.created",
                PUBLISHED_AT,
                METADATA,
                "payload"
        );

        AuditPublisher required = new DefaultAuditPublisher(
                registry,
                entry -> {
                    throw new IllegalStateException("sink unavailable");
                }
        );
        AuditPublicationException failure = assertThrows(
                AuditPublicationException.class,
                () -> required.publish(envelope)
        );
        assertSame(envelope, failure.envelope());

        List<AuditEnvelope<?>> failedEnvelopes = new ArrayList<>();
        AuditPublisher bestEffort = new DefaultAuditPublisher(
                registry,
                entry -> {
                    throw new IllegalStateException("sink unavailable");
                },
                Clock.systemUTC(),
                eventName -> EVENT_ID,
                () -> METADATA,
                AuditDeliveryPolicy.BEST_EFFORT,
                (failedEnvelope, entry, ignored) -> failedEnvelopes.add(failedEnvelope)
        );
        bestEffort.publish(envelope);
        assertEquals(List.of(envelope), failedEnvelopes);
    }
}
