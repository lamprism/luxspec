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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable typed operation attributes.
 *
 * <p>Every value is checked against its attribute specification and formatter
 * before it is stored. The builder rejects duplicate assignments, and the set
 * is structurally immutable after construction.
 *
 * @author RollW
 */
public final class ObservationAttributeSet {
    private static final ObservationAttributeSet EMPTY = new ObservationAttributeSet(Map.of());
    private final Map<ObservationAttributeSpec<?>, Object> values;

    private ObservationAttributeSet(Map<ObservationAttributeSpec<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static ObservationAttributeSet empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Copies a validated attribute map into an immutable set.
     *
     * @param values the typed attribute assignments
     * @return the immutable set
     */
    public static ObservationAttributeSet copyOf(Map<ObservationAttributeSpec<?>, ?> values) {
        Objects.requireNonNull(values, "values");
        Map<ObservationAttributeSpec<?>, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<ObservationAttributeSpec<?>, ?> entry : values.entrySet()) {
            ObservationAttributeSpec<?> spec = Objects.requireNonNull(entry.getKey(), "attribute spec");
            Object value = Objects.requireNonNull(entry.getValue(), "attribute value");
            validateValue(spec, value);
            copied.put(spec, value);
        }
        return new ObservationAttributeSet(copied);
    }

    public Map<ObservationAttributeSpec<?>, Object> values() {
        return values;
    }

    public <T> Optional<T> get(ObservationAttributeSpec<T> spec) {
        Objects.requireNonNull(spec, "spec");
        Object value = values.get(spec);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(spec.valueType().cast(value));
    }

    public ObservationAttributeSet with(ObservationAttributeSet additions) {
        Objects.requireNonNull(additions, "additions");
        Map<ObservationAttributeSpec<?>, Object> merged = new LinkedHashMap<>(values);
        merged.putAll(additions.values);
        return new ObservationAttributeSet(merged);
    }

    /**
     * Builds an immutable attribute set.
     */
    public static final class Builder {
        private final Map<ObservationAttributeSpec<?>, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        public <T> Builder put(ObservationAttributeSpec<T> spec, T value) {
            ObservationAttributeSpec<T> nonNullSpec = Objects.requireNonNull(spec, "spec");
            Objects.requireNonNull(value, "value");
            if (values.containsKey(nonNullSpec)) {
                throw new IllegalArgumentException("Observation attribute is assigned more than once: " + nonNullSpec.name());
            }
            nonNullSpec.format(value);
            values.put(nonNullSpec, value);
            return this;
        }

        public ObservationAttributeSet build() {
            return new ObservationAttributeSet(values);
        }
    }

    private static <T> void validateValue(ObservationAttributeSpec<T> spec, Object value) {
        spec.format(spec.valueType().cast(value));
    }
}
