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

import com.lamprism.luxspec.validation.ValidationRules;
import com.lamprism.luxspec.validation.Validator;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Sanitized framework-neutral accountability actor.
 *
 * <p>The kind is required. The optional ID is preserved as supplied and, when
 * present, must not be blank. Audit does not interpret the ID as a username,
 * UUID, credential, or framework security object.
 *
 * @author RollW
 */
public final class AuditActor {
    public enum Kind {
        USER,
        SERVICE,
        SYSTEM,
        ANONYMOUS,
        UNKNOWN
    }

    public static final AuditActor UNKNOWN = new AuditActor(Kind.UNKNOWN, null);

    private static final Validator<String> ID_VALIDATOR = ValidationRules.nonBlank("Audit actor ID");

    private final Kind kind;
    private final @Nullable String id;

    private AuditActor(Kind kind, @Nullable String id) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (id != null) {
            ID_VALIDATOR.validate(id);
        }
        this.id = id;
    }

    public static AuditActor of(Kind kind, @Nullable String id) {
        return new AuditActor(kind, id);
    }

    public static AuditActor unknown() {
        return UNKNOWN;
    }

    public Kind kind() {
        return kind;
    }

    public @Nullable String id() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuditActor actor)) {
            return false;
        }
        return kind == actor.kind && Objects.equals(id, actor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, id);
    }
}
