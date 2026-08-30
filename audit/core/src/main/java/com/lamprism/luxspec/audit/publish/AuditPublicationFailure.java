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

package com.lamprism.luxspec.audit.publish;

import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditEnvelope;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Identifies the audit publication state associated with a best-effort failure.
 *
 * @author RollW
 */
public final class AuditPublicationFailure {
    private final AuditEnvelope<?> envelope;
    private final @Nullable AuditEntry entry;

    /**
     * Creates an audit publication failure context.
     *
     * @param envelope the publication envelope
     * @param entry    the translated entry, or {@code null} when translation failed
     */
    public AuditPublicationFailure(AuditEnvelope<?> envelope, @Nullable AuditEntry entry) {
        this.envelope = Objects.requireNonNull(envelope, "envelope");
        this.entry = entry;
    }

    /**
     * Returns the publication envelope.
     *
     * @return the envelope
     */
    public AuditEnvelope<?> getEnvelope() {
        return envelope;
    }

    /**
     * Returns the translated entry when translation completed.
     *
     * @return the entry, or {@code null} when translation failed
     */
    public @Nullable AuditEntry getEntry() {
        return entry;
    }
}
