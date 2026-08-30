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
 * required. The event name follows {@link AuditNameNormalizer}. The
 * envelope preserves its supplied identity and metadata; it does not copy the
 * payload or infer missing publication metadata.
 *
 * @author RollW
 */
public final class AuditEnvelope<E> {
    private static final AuditNameNormalizer EVENT_NAME_NORMALIZER = AuditNameNormalizer.instance();
    private final AuditEventId id;
    private final String eventName;
    private final Instant publishedAt;
    private final AuditMetadata metadata;
    private final E event;

    /**
     * Creates an immutable publication envelope.
     *
     * @param id          the stable source event identity
     * @param eventName   the stable audit event name
     * @param publishedAt the time the event entered audit publication
     * @param metadata    the captured publication metadata
     * @param event       the event payload
     */
    public AuditEnvelope(
            AuditEventId id,
            String eventName,
            Instant publishedAt,
            AuditMetadata metadata,
            E event
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.eventName = EVENT_NAME_NORMALIZER.normalize(eventName);
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.event = Objects.requireNonNull(event, "event");
    }

    /**
     * Returns the stable source event identity.
     *
     * @return the audit event ID
     */
    public AuditEventId id() {
        return id;
    }

    /**
     * Returns the stable audit event name.
     *
     * @return the audit event name
     */
    public String eventName() {
        return eventName;
    }

    /**
     * Returns the time the event entered audit publication.
     *
     * @return the publication time
     */
    public Instant publishedAt() {
        return publishedAt;
    }

    /**
     * Returns the metadata captured for this publication.
     *
     * @return the audit metadata
     */
    public AuditMetadata metadata() {
        return metadata;
    }

    /**
     * Returns the immutable event payload supplied by the caller.
     *
     * @return the event payload
     */
    public E event() {
        return event;
    }
}
