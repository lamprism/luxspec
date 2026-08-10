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

package com.lamprism.luxspec.audit;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable audit publication input preserved across retries and replays.
 *
 * <p>The event ID, publication time, metadata, event name, and payload are all
 * required. The event name follows {@link AuditNameValidator#require(String)}. The
 * envelope preserves its supplied identity and metadata; it does not copy the
 * payload or read ambient context.
 *
 * @author RollW
 */
public final class AuditEnvelope<E> {
    private final AuditEventId id;
    private final String eventName;
    private final Instant publishedAt;
    private final AuditMetadata metadata;
    private final E event;

    public AuditEnvelope(
            AuditEventId id,
            String eventName,
            Instant publishedAt,
            AuditMetadata metadata,
            E event
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.eventName = AuditNameValidator.require(eventName);
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.event = Objects.requireNonNull(event, "event");
    }

    public AuditEventId id() {
        return id;
    }

    public String eventName() {
        return eventName;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public AuditMetadata metadata() {
        return metadata;
    }

    public E event() {
        return event;
    }
}
