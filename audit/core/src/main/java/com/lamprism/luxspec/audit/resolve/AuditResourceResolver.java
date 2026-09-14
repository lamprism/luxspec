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

package com.lamprism.luxspec.audit.resolve;

import com.lamprism.luxspec.audit.AuditEnvelope;
import com.lamprism.luxspec.resource.ResourceReference;
import org.jspecify.annotations.Nullable;

/**
 * Resolves stable resource identity for one translated event.
 *
 * <p>The resolver returns a stable reference rather than loading or retaining
 * a domain object. Returning null means that the event has no resource
 * identity.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuditResourceResolver<E> {
    /**
     * Resolves the resource reference represented by an event envelope.
     *
     * <p>Implementations should derive identity from stable event data and must
     * not load a mutable domain object merely to return its reference.</p>
     *
     * @param envelope the immutable publication envelope
     * @return the stable resource reference, or {@code null} when the event has no resource
     */
    @Nullable
    ResourceReference<?> resolve(AuditEnvelope<E> envelope);
}
