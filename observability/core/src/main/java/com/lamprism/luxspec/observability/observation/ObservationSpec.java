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

package com.lamprism.luxspec.observability.observation;

import com.lamprism.luxspec.validation.Validator;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable declaration of one operation observation.
 *
 * <p>The observation name follows {@link ObservationName#of(String)}. A
 * description is optional; when supplied it is trimmed and must not be empty.
 * The default observation kind is {@link ObservationKind#INTERNAL}.
 *
 * @author RollW
 */
public final class ObservationSpec {
    private final ObservationName name;
    private final ObservationKind kind;
    private final @Nullable String description;

    private ObservationSpec(Builder builder) {
        name = builder.name;
        kind = builder.kind;
        description = builder.description;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static Builder builder(ObservationName name) {
        return new Builder(Objects.requireNonNull(name, "name"));
    }

    public ObservationName getName() {
        return name;
    }

    public ObservationKind getKind() {
        return kind;
    }

    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ObservationSpec spec)) {
            return false;
        }
        return name.equals(spec.name) && kind == spec.kind && Objects.equals(description, spec.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind, description);
    }

    /**
     * Builds an immutable observation declaration.
     */
    public static final class Builder {
        private final ObservationName name;
        private ObservationKind kind = ObservationKind.INTERNAL;
        private @Nullable String description;

        private Builder(String name) {
            this(ObservationName.of(name));
        }

        private Builder(ObservationName name) {
            this.name = name;
        }

        public Builder kind(ObservationKind kind) {
            this.kind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        public Builder description(String description) {
            String normalized = Objects.requireNonNull(description, "description").trim();
            Validator.nonBlank("Observation description").validate(normalized);
            this.description = normalized;
            return this;
        }

        public ObservationSpec build() {
            return new ObservationSpec(this);
        }
    }
}
