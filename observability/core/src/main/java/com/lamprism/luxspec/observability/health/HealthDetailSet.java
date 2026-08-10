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

package com.lamprism.luxspec.observability.health;

import com.lamprism.luxspec.validation.ValidationRules;
import com.lamprism.luxspec.validation.Validator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable named health detail values.
 *
 * <p>Detail names must be non-empty and must not contain whitespace, space
 * characters, or ISO control characters. Names are not trimmed. Values must be
 * non-null, and duplicate names are rejected by the builder.
 *
 * @author RollW
 */
public final class HealthDetailSet {
    private static final HealthDetailSet EMPTY = new HealthDetailSet(Map.of());
    private static final Validator<String> NAME_VALIDATOR = ValidationRules
            .nonBlank("Health detail name")
            .and(ValidationRules.noWhitespace("Health detail name"))
            .and(ValidationRules.noControlCharacters("Health detail name"));

    private final Map<String, Object> values;

    private HealthDetailSet(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static HealthDetailSet empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> values() {
        return values;
    }

    public Optional<Object> get(String name) {
        return Optional.ofNullable(values.get(Objects.requireNonNull(name, "name")));
    }

    /**
     * Builds an immutable detail set.
     */
    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder put(String name, Object value) {
            String normalized = validateName(name);
            Objects.requireNonNull(value, "value");
            if (values.containsKey(normalized)) {
                throw new IllegalArgumentException("Health detail is assigned more than once: " + normalized);
            }
            values.put(normalized, value);
            return this;
        }

        public HealthDetailSet build() {
            return values.isEmpty() ? EMPTY : new HealthDetailSet(values);
        }

        private static String validateName(String name) {
            String nonNullName = Objects.requireNonNull(name, "name");
            NAME_VALIDATOR.validate(nonNullName);
            return nonNullName;
        }
    }
}
