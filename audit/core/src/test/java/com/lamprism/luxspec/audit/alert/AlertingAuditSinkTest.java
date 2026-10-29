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

package com.lamprism.luxspec.audit.alert;

import com.lamprism.luxspec.audit.AuditAction;
import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditMetadata;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.store.InMemoryAuditStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class AlertingAuditSinkTest {
    @Test
    void deliversSelectedAlertsOnlyAfterAuditAcceptance() {
        InMemoryAuditStore store = new InMemoryAuditStore();
        List<String> alerts = new ArrayList<>();
        AlertingAuditSink<String> sink = new AlertingAuditSink<>(
                store,
                AuditAlertPolicy.forOutcomes(Set.of(AuditOutcome.DENIED), entry -> entry.id().value()),
                alerts::add
        );
        AuditEntry denied = entry("denied", AuditOutcome.DENIED);
        sink.accept(entry("allowed", AuditOutcome.SUCCESS));
        sink.accept(denied);
        sink.accept(denied);

        assertEquals(2, store.size());
        assertEquals(List.of("denied", "denied"), alerts);
    }

    @Test
    void neverAlertsWhenAuditAcceptanceFails() {
        List<String> alerts = new ArrayList<>();
        AlertingAuditSink<String> sink = new AlertingAuditSink<>(
                entry -> {
                    throw new IllegalStateException("Audit unavailable");
                },
                entry -> Optional.of(entry.id().value()),
                alerts::add
        );

        assertThrows(IllegalStateException.class, () -> sink.accept(entry("denied", AuditOutcome.DENIED)));
        assertEquals(List.of(), alerts);
    }

    @Test
    void propagatesOrReportsAlertDeliveryFailureAfterAcceptance() {
        InMemoryAuditStore store = new InMemoryAuditStore();
        RuntimeException failure = new IllegalStateException("Alert unavailable");
        AuditEntry entry = entry("denied", AuditOutcome.DENIED);
        AlertingAuditSink<String> required = new AlertingAuditSink<>(
                store, ignored -> Optional.of("alert"), alert -> {
            throw failure;
        }
        );
        assertSame(failure, assertThrows(IllegalStateException.class, () -> required.accept(entry)));
        assertEquals(List.of(entry), store.entries());

        AtomicReference<Throwable> reported = new AtomicReference<>();
        AlertingAuditSink<String> reporting = new AlertingAuditSink<>(
                store, ignored -> Optional.of("alert"), alert -> {
            throw failure;
        }, (accepted, cause) -> {
            assertSame(entry, accepted);
            reported.set(cause);
        }
        );
        reporting.accept(entry);
        assertSame(failure, reported.get());
    }

    private static AuditEntry entry(String id, AuditOutcome outcome) {
        Instant now = Instant.parse("2026-07-15T00:00:00Z");
        return new AuditEntry(
                AuditEventId.of(id), "security.authentication", now, now,
                new AuditMetadata(AuditActor.unknown(), null, AuditFieldSet.empty()),
                AuditAction.of("authenticate"), outcome, null, AuditFieldSet.empty()
        );
    }
}
