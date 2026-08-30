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

import com.lamprism.luxspec.resource.ResourceReference;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable provider-independent accountability entry.
 *
 * <p>The event name is normalized by {@link AuditNameNormalizer}. The
 * entry preserves the envelope ID, publication time, and metadata while the
 * translator supplies the occurrence time, action, outcome, resource, and
 * fields.
 *
 * @author RollW
 */
public final class AuditEntry {
    private static final AuditNameNormalizer EVENT_NAME_NORMALIZER = AuditNameNormalizer.instance();
    private final AuditEventId id;
    private final String eventName;
    private final Instant occurredAt;
    private final Instant publishedAt;
    private final AuditMetadata metadata;
    private final AuditAction action;
    private final AuditOutcome outcome;
    private final @Nullable ResourceReference<?> resource;
    private final AuditFieldSet fields;

    /**
     * Creates one complete immutable audit entry.
     *
     * <p>Publishers and translators should normally use {@link #from(AuditEnvelope,
     * AuditEntryContent)} so ownership of publication metadata remains explicit.</p>
     *
     * @param id          the stable source event identity
     * @param eventName   the stable audit event name
     * @param occurredAt  the time the audited operation occurred
     * @param publishedAt the time the event entered audit publication
     * @param metadata    the captured publication metadata
     * @param action      the semantic audit action
     * @param outcome     the operation outcome
     * @param resource    the affected resource, or {@code null} when no resource applies
     * @param fields      additional typed audit fields
     */
    public AuditEntry(
            AuditEventId id,
            String eventName,
            Instant occurredAt,
            Instant publishedAt,
            AuditMetadata metadata,
            AuditAction action,
            AuditOutcome outcome,
            @Nullable ResourceReference<?> resource,
            AuditFieldSet fields
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.eventName = EVENT_NAME_NORMALIZER.normalize(eventName);
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.action = Objects.requireNonNull(action, "action");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.resource = resource;
        this.fields = Objects.requireNonNull(fields, "fields");
    }

    /**
     * Combines translator-owned content with the immutable publication envelope.
     *
     * @param envelope the event identity and publication metadata
     * @param content  the translated semantic content
     * @param <E>      the event payload type
     * @return the complete audit entry
     */
    public static <E> AuditEntry from(AuditEnvelope<E> envelope, AuditEntryContent content) {
        AuditEnvelope<E> nonNullEnvelope = Objects.requireNonNull(envelope, "envelope");
        AuditEntryContent nonNullContent = Objects.requireNonNull(content, "content");
        return new AuditEntry(
                nonNullEnvelope.id(),
                nonNullEnvelope.eventName(),
                nonNullContent.occurredAt(),
                nonNullEnvelope.publishedAt(),
                nonNullEnvelope.metadata(),
                nonNullContent.action(),
                nonNullContent.outcome(),
                nonNullContent.resource(),
                nonNullContent.fieldSet()
        );
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
     * Returns the time the audited operation occurred.
     *
     * @return the occurrence time
     */
    public Instant occurredAt() {
        return occurredAt;
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
     * Returns the captured publication metadata.
     *
     * @return the audit metadata
     */
    public AuditMetadata metadata() {
        return metadata;
    }

    /**
     * Returns the semantic audit action.
     *
     * @return the audit action
     */
    public AuditAction action() {
        return action;
    }

    /**
     * Returns the audited operation outcome.
     *
     * @return the audit outcome
     */
    public AuditOutcome outcome() {
        return outcome;
    }

    /**
     * Returns the optional affected resource.
     *
     * @return the resource reference, or {@code null} when no resource applies
     */
    public @Nullable ResourceReference<?> resource() {
        return resource;
    }

    /**
     * Returns the additional typed audit fields.
     *
     * @return the immutable field set
     */
    public AuditFieldSet fields() {
        return fields;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuditEntry entry)) {
            return false;
        }
        return id.equals(entry.id)
                && eventName.equals(entry.eventName)
                && occurredAt.equals(entry.occurredAt)
                && publishedAt.equals(entry.publishedAt)
                && metadata.equals(entry.metadata)
                && action.equals(entry.action)
                && outcome == entry.outcome
                && Objects.equals(resource, entry.resource)
                && fields.equals(entry.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                eventName,
                occurredAt,
                publishedAt,
                metadata,
                action,
                outcome,
                resource,
                fields
        );
    }
}
