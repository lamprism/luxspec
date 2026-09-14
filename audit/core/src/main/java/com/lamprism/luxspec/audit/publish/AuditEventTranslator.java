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

import com.lamprism.luxspec.audit.AuditEntryContent;
import com.lamprism.luxspec.audit.AuditEnvelope;
import org.jspecify.annotations.Nullable;

/**
 * Translates one typed envelope into zero or one audit entry content values.
 *
 * <p>A null result means that the event is not auditable. A translator must
 * not generate a replacement event ID, publication time, or metadata; those
 * values are preserved from the envelope by the publisher.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuditEventTranslator<E> {
    /**
     * Translates one immutable publication envelope.
     *
     * <p>Implementations should be deterministic for the same envelope so a
     * replay preserves audit semantics. They should not mutate or retain the
     * event payload, perform sink delivery, or replace publisher-owned
     * identity and metadata. A runtime failure is handled according to the
     * publisher's delivery policy.</p>
     *
     * @param envelope the event and publisher-owned audit metadata
     * @return translated entry content, or {@code null} when this event instance should not be audited
     */
    @Nullable
    AuditEntryContent translate(AuditEnvelope<E> envelope);
}
