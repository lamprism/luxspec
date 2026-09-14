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

import com.lamprism.luxspec.validation.Validator;

import java.util.Objects;

/**
 * Stable identity and idempotency key for one audit source event.
 *
 * <p>{@link #of(String)} rejects null or blank values but does not trim or
 * otherwise normalize a non-blank value. The value does not need to be a UUID.
 * The value object keeps event identity distinct from event names and other
 * identifiers while centralizing the non-blank invariant.
 *
 * @author RollW
 */
public final class AuditEventId {
    private static final Validator<String> VALUE_VALIDATOR = Validator.nonBlank("Audit event ID");

    private final String value;

    private AuditEventId(String value) {
        this.value = value;
    }

    public static AuditEventId of(String value) {
        VALUE_VALIDATOR.validate(Objects.requireNonNull(value, "value"));
        return new AuditEventId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuditEventId id)) {
            return false;
        }
        return value.equals(id.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
