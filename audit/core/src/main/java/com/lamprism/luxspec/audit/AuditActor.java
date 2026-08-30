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

import com.lamprism.luxspec.validation.Normalizer;
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
    /**
     * Extensible actor classification used for provider-independent metadata.
     *
     * <p>The standard constants cover common actor categories. Applications
     * may create another validated kind when their domain needs a category
     * that does not fit those constants.</p>
     */
    public static final class Kind {
        private static final Normalizer<String> VALUE_NORMALIZER = Normalizer.of(String::trim)
                .validatedBy(
                        ValidationRules.nonBlank("Audit actor kind")
                                .and(ValidationRules.noWhitespace("Audit actor kind"))
                                .and(ValidationRules.noControlCharacters("Audit actor kind"))
                );

        /**
         * A human user acting through an application boundary.
         */
        public static final Kind USER = of("USER");
        /** A service or workload identity acting without a human session. */
        public static final Kind SERVICE = of("SERVICE");
        /** The application or platform performing an internal operation. */
        public static final Kind SYSTEM = of("SYSTEM");
        /** An unauthenticated actor whose absence of identity is known. */
        public static final Kind ANONYMOUS = of("ANONYMOUS");
        /** An actor whose classification could not be established. */
        public static final Kind UNKNOWN = of("UNKNOWN");

        private final String value;

        private Kind(String value) {
            this.value = value;
        }

        /**
         * Creates an application-defined actor classification.
         *
         * @param value the non-blank classification without whitespace or control characters
         * @return the actor classification
         */
        public static Kind of(String value) {
            return new Kind(VALUE_NORMALIZER.normalize(value));
        }

        /**
         * Returns the stable classification value.
         *
         * @return the classification value
         */
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Kind kind)) {
                return false;
            }
            return value.equals(kind.value);
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

    /** The canonical actor used when neither classification nor identity is available. */
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

    /**
     * Creates an actor from a classification and optional provider-neutral identifier.
     *
     * @param kind the actor classification
     * @param id   the actor identifier, or {@code null} when no identifier is available
     * @return the audit actor
     */
    public static AuditActor of(Kind kind, @Nullable String id) {
        return new AuditActor(kind, id);
    }

    /**
     * Returns the canonical unknown actor.
     *
     * @return the unknown actor
     */
    public static AuditActor unknown() {
        return UNKNOWN;
    }

    /**
     * Returns the actor classification.
     *
     * @return the actor classification
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the optional actor identifier.
     *
     * @return the identifier, or {@code null} when unavailable
     */
    public @Nullable String id() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuditActor actor)) {
            return false;
        }
        return kind.equals(actor.kind) && Objects.equals(id, actor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, id);
    }
}
