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

import com.lamprism.luxspec.audit.AuditDetail;
import com.lamprism.luxspec.audit.AuditEnvelope;
import org.jspecify.annotations.Nullable;

/**
 * Resolves an opaque application-owned details carrier.
 *
 * <p>The resolver is explicit and typed. It does not use broad reflection or
 * implicit serialization; returning null means that no details are attached.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuditDetailResolver<E, D> {
    @Nullable
    AuditDetail<D> resolve(AuditEnvelope<E> envelope);
}
