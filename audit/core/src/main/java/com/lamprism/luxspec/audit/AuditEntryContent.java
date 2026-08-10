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
    private final AuditAction action;
    private final AuditOutcome outcome;
    private final @Nullable ResourceReference<?> resource;
    private final AuditFieldSet fields;
    private final @Nullable AuditDetail<?> details;

    public AuditEntryContent(
            AuditAction action,
            AuditOutcome outcome,
            @Nullable ResourceReference<?> resource,
            AuditFieldSet fields,
            @Nullable AuditDetail<?> details
    ) {
        this.action = Objects.requireNonNull(action, "action");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.resource = resource;
        this.fields = Objects.requireNonNull(fields, "fields");
        this.details = details;
    }

    public static AuditEntryContent of(
            AuditAction action,
            AuditOutcome outcome,
            @Nullable ResourceReference<?> resource,
            AuditFieldSet fields,
            @Nullable AuditDetail<?> details
    ) {
        return new AuditEntryContent(action, outcome, resource, fields, details);
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
