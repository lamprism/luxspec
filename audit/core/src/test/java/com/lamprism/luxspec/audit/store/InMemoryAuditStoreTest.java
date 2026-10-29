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

package com.lamprism.luxspec.audit.store;

import com.lamprism.luxspec.audit.AuditAction;
import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditMetadata;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.retention.AuditRetentionPolicy;
import com.lamprism.luxspec.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class InMemoryAuditStoreTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-15T00:00:00Z");
    private static final Instant PUBLISHED_AT = OCCURRED_AT.plusSeconds(1L);

    @Test
    void acceptsAnEquivalentReplayWithoutDuplicatingIt() {
        InMemoryAuditStore store = new InMemoryAuditStore();
        AuditEntry entry = entry("user.registered");

        store.accept(entry);
        store.accept(entry("user.registered"));

        assertEquals(List.of(entry), store.entries());
        assertEquals(1, store.size());
    }

    @Test
    void rejectsAnEventIdThatIsReusedForDifferentContent() {
        InMemoryAuditStore store = new InMemoryAuditStore();
        store.accept(entry("user.registered"));

        assertThrows(IllegalStateException.class, () -> store.accept(entry("user.disabled")));
    }

    @Test
    void removesOnlySelectedEntriesInBoundedBatches() {
        InMemoryAuditStore store = new InMemoryAuditStore();
        AuditEntry first = entry("event-1", "user.registered", PUBLISHED_AT);
        AuditEntry held = entry("event-2", "user.held", PUBLISHED_AT);
        AuditEntry second = entry("event-3", "user.registered", PUBLISHED_AT.plusSeconds(1));
        store.accept(first);
        store.accept(held);
        store.accept(second);
        AuditRetentionPolicy age = AuditRetentionPolicy.afterPublication(Duration.ofDays(30));
        AuditRetentionPolicy policy = (entry, now) -> !entry.eventName().equals("user.held")
                && age.shouldRemove(entry, now);

        assertEquals(1, store.remove(policy, PUBLISHED_AT.plus(Duration.ofDays(30)).plusSeconds(1), 1));
        assertEquals(List.of(held, second), store.entries());
        assertEquals(1, store.remove(policy, PUBLISHED_AT.plus(Duration.ofDays(30)).plusSeconds(1), 10));
        assertEquals(List.of(held), store.entries());
        assertThrows(IllegalArgumentException.class, () -> store.remove(policy, PUBLISHED_AT, 0));
    }

    @Test
    void doesNotRemoveAnyEntriesWhenPolicyEvaluationFails() {
        InMemoryAuditStore store = new InMemoryAuditStore();
        AuditEntry first = entry("event-1", "user.registered", PUBLISHED_AT);
        AuditEntry second = entry("event-2", "user.disabled", PUBLISHED_AT);
        store.accept(first);
        store.accept(second);

        assertThrows(IllegalStateException.class, () -> store.remove((entry, now) -> {
            if (entry.id().equals(second.id())) {
                throw new IllegalStateException("Retention policy failed");
            }
            return true;
        }, PUBLISHED_AT, 10));

        assertEquals(List.of(first, second), store.entries());
    }

    private static AuditEntry entry(String eventName) {
        return entry("event-1", eventName, PUBLISHED_AT);
    }

    private static AuditEntry entry(String id, String eventName, Instant publishedAt) {
        return new AuditEntry(
                AuditEventId.of(id),
                eventName,
                OCCURRED_AT,
                publishedAt,
                new AuditMetadata(
                        AuditActor.of(AuditActor.Kind.USER, "42"),
                        CorrelationId.of("request-1"),
                        AuditFieldSet.empty()
                ),
                AuditAction.of("user.register"),
                AuditOutcome.SUCCESS,
                null,
                AuditFieldSet.empty()
        );
    }
}
