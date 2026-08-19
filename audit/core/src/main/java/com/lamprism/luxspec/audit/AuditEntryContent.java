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
 * Translator-owned semantic content for one final audit entry.
 *
 * <p>This value contains only the semantic result of translation. Event
 * identity, event name, publication time, and metadata remain owned by the
 * corresponding {@link AuditEnvelope}.
 *
 * @author RollW
 */
public final class AuditEntryContent {
    private final Instant occurredAt;
    private final AuditAction action;
    private final AuditOutcome outcome;
    private final @Nullable ResourceReference<?> resource;
    private final AuditFieldSet fieldSet;
    private final @Nullable AuditDetail<?> detail;

    public AuditEntryContent(
            Instant occurredAt,
            AuditAction action,
            AuditOutcome outcome,
            @Nullable ResourceReference<?> resource,
            AuditFieldSet fieldSet,
            @Nullable AuditDetail<?> detail
    ) {
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.action = Objects.requireNonNull(action, "action");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.resource = resource;
        this.fieldSet = Objects.requireNonNull(fieldSet, "fieldSet");
        this.detail = detail;
    }

    public static AuditEntryContent of(
            Instant occurredAt,
            AuditAction action,
            AuditOutcome outcome,
            @Nullable ResourceReference<?> resource,
            AuditFieldSet fieldSet,
            @Nullable AuditDetail<?> detail
    ) {
        return new AuditEntryContent(occurredAt, action, outcome, resource, fieldSet, detail);
    }

    public Instant occurredAt() {
        return occurredAt;
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

    public AuditFieldSet fieldSet() {
        return fieldSet;
    }

    public @Nullable AuditDetail<?> detail() {
        return detail;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuditEntryContent content)) {
            return false;
        }
        return occurredAt.equals(content.occurredAt)
                && action.equals(content.action)
                && outcome == content.outcome
                && Objects.equals(resource, content.resource)
                && fieldSet.equals(content.fieldSet)
                && Objects.equals(detail, content.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(occurredAt, action, outcome, resource, fieldSet, detail);
    }
}
