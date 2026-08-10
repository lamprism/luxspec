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

import java.util.Objects;

/**
 * Opaque application-owned audit detail carrier.
 *
 * <p>The carried value must be non-null. Audit does not serialize, inspect,
 * redact, copy, or impose a size limit on the value; the resolver, sink, or
 * transport that owns the boundary defines those policies.
 *
 * @author RollW
 */
public final class AuditDetail<D> {
    private final D value;

    public AuditDetail(D value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public D value() {
        return value;
    }
}
