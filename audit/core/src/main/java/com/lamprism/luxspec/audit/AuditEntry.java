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
 * <p>The event name is normalized by {@link AuditNameValidator#require(String)}. The
 * entry preserves the envelope ID, publication time, and metadata while the
 * translator supplies the action, outcome, resource, fields, and optional
 * details.
 *
 * @author RollW
 */
public final class AuditEntry {
    private final AuditEventId id;
    private final String eventName;
    private final Instant occurredAt;
    private final AuditMetadata metadata;
    private final AuditAction action;
    private final AuditOutcome outcome;
    private final @Nullable ResourceReference<?> resource;
    private final AuditFieldSet fields;
    private final @Nullable AuditDetail<?> details;

    public AuditEntry(
            AuditEventId id,
            String eventName,
            Instant occurredAt,
            AuditMetadata metadata,
            AuditAction action,
            AuditOutcome outcome,
            @Nullable ResourceReference<?> resource,
            AuditFieldSet fields,
            @Nullable AuditDetail<?> details
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.eventName = AuditNameValidator.require(eventName);
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.action = Objects.requireNonNull(action, "action");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.resource = resource;
        this.fields = Objects.requireNonNull(fields, "fields");
        this.details = details;
    }

    public AuditEventId id() {
        return id;
    }

    public String eventName() {
        return eventName;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public AuditMetadata metadata() {
        return metadata;
    }

    public AuditAction action() {
        return action;
    }

    public AuditOutcome outcome() {
        return outcome;
    }

    public @Nullable ResourceReference<?> resource() {
        return resource;
    }

    public AuditFieldSet fields() {
        return fields;
    }

    public @Nullable AuditDetail<?> details() {
        return details;
    }
}
