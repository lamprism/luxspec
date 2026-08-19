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
import com.lamprism.luxspec.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static AuditEntry entry(String eventName) {
        return new AuditEntry(
                AuditEventId.of("event-1"),
                eventName,
                OCCURRED_AT,
                PUBLISHED_AT,
                new AuditMetadata(
                        AuditActor.of(AuditActor.Kind.USER, "42"),
                        CorrelationId.of("request-1"),
                        AuditFieldSet.empty()
                ),
                AuditAction.of("user.register"),
                AuditOutcome.SUCCESS,
                null,
                AuditFieldSet.empty(),
                null
        );
    }
}
