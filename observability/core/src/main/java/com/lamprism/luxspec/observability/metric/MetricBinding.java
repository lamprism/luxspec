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

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable concrete metric identity produced by a MetricSpec binding.
 *
 * <p>Identity consists of the spec and formatted dimension values. A retained
 * value source is auxiliary state and is removed by {@link #withoutValueSource()};
 * it does not affect equality or hash code.
 *
 * @author RollW
 */
public final class MetricBinding<M extends Metric> {
    private final MetricSpec<M> spec;
    private final Map<MetricDimensionSpec<?>, Object> values;
    private final Map<String, String> formattedDimensions;
    private final @Nullable Object valueSource;

    MetricBinding(
            MetricSpec<M> spec,
            Map<MetricDimensionSpec<?>, Object> values,
            Map<String, String> formattedDimensions,
            @Nullable Object valueSource
    ) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.values = Map.copyOf(values);
        this.formattedDimensions = Map.copyOf(new LinkedHashMap<>(formattedDimensions));
        this.valueSource = valueSource;
    }

    public MetricSpec<M> getSpec() {
        return spec;
    }

    public Map<MetricDimensionSpec<?>, Object> getValues() {
        return values;
    }

    public Map<String, String> getDimensions() {
        return formattedDimensions;
    }

    public List<MetricDimensionSpec<?>> getDimensionSpecs() {
        return spec.getDimensions();
    }

    public @Nullable Object getValueSource() {
        return valueSource;
    }

    /**
     * Returns the same metric identity without retaining a value source.
     *
     * @return a source-free binding
     */
    public MetricBinding<M> withoutValueSource() {
        if (valueSource == null) {
            return this;
        }
        return new MetricBinding<>(spec, values, formattedDimensions, null);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricBinding<?> binding)) {
            return false;
        }
        return spec.equals(binding.spec) && formattedDimensions.equals(binding.formattedDimensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, formattedDimensions);
    }

    @Override
    public String toString() {
        return spec.getName().getValue() + formattedDimensions;
    }
}
