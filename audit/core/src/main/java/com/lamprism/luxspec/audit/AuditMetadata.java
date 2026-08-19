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

import com.lamprism.luxspec.context.CorrelationId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Publication-time accountability context snapshot.
 *
 * <p>The actor and custom field set are required. The correlation ID is
 * optional and is only an association value; provider-specific trace context,
 * credentials, and tenant semantics are outside this type.
 *
 * @author RollW
 */
public final class AuditMetadata {
    private final AuditActor actor;
    private final @Nullable CorrelationId correlationId;
    private final AuditFieldSet fields;

    public AuditMetadata(
            AuditActor actor,
            @Nullable CorrelationId correlationId,
            AuditFieldSet fields
    ) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.correlationId = correlationId;
        this.fields = Objects.requireNonNull(fields, "fields");
    }

    public AuditActor actor() {
        return actor;
    }

    public @Nullable CorrelationId correlationId() {
        return correlationId;
    }

    public AuditFieldSet fields() {
        return fields;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuditMetadata metadata)) {
            return false;
        }
        return actor.equals(metadata.actor)
                && Objects.equals(correlationId, metadata.correlationId)
                && fields.equals(metadata.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actor, correlationId, fields);
    }
}
