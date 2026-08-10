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

import com.lamprism.luxspec.audit.AuditEnvelope;

/**
 * Publishes audit events through a configured translation and delivery path.
 *
 * <p>The event publication contract is intentionally independent of the
 * default in-process implementation. Applications may provide an
 * implementation for an outbox, transaction-aware delivery, asynchronous
 * delivery, or a publisher decorator.</p>
 *
 * @author RollW
 */
public interface AuditPublisher {
    /**
     * Publishes a new local event and creates its publication envelope.
     *
     * @param eventName the validated audit event name
     * @param event     the event payload
     * @param <E>       the event payload type
     */
    <E> void publish(String eventName, E event);

    /**
     * Publishes an existing envelope without replacing its identity or
     * publication metadata.
     *
     * @param envelope the immutable event envelope
     * @param <E>      the event payload type
     */
    <E> void publish(AuditEnvelope<E> envelope);
}
