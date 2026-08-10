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

package com.lamprism.luxspec.observability.metric;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable typed assignments for a metric binding.
 *
 * <p>Each assignment uses a declared {@link MetricDimensionSpec} as its typed
 * key. Null values, type mismatches, and duplicate assignments are rejected;
 * the set does not infer missing dimensions or discard extra dimensions during
 * binding.
 *
 * @author RollW
 */
public final class MetricDimensionSet {
    private static final MetricDimensionSet EMPTY = new MetricDimensionSet(Map.of());

    private final Map<MetricDimensionSpec<?>, Object> values;

    private MetricDimensionSet(Map<MetricDimensionSpec<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static MetricDimensionSet empty() {
        return EMPTY;
    }

    public static <T> MetricDimensionSet of(MetricDimensionSpec<T> spec, T value) {
        return builder().put(spec, value).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<MetricDimensionSpec<?>, Object> values() {
        return values;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricDimensionSet dimensionSet)) {
            return false;
        }
        return values.equals(dimensionSet.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    /**
     * Builds an immutable assignment set.
     */
    public static final class Builder {
        private final Map<MetricDimensionSpec<?>, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        public <T> Builder put(MetricDimensionSpec<T> spec, T value) {
            MetricDimensionSpec<T> nonNullSpec = Objects.requireNonNull(spec, "spec");
            Objects.requireNonNull(value, "value");
            if (!nonNullSpec.valueType().isInstance(value)) {
                throw new IllegalArgumentException("Metric dimension value does not match type: " + nonNullSpec.name());
            }
            if (values.putIfAbsent(nonNullSpec, value) != null) {
                throw new IllegalArgumentException("Metric dimension is assigned more than once: " + nonNullSpec.name());
            }
            return this;
        }

        public MetricDimensionSet build() {
            return new MetricDimensionSet(values);
        }
    }
}
