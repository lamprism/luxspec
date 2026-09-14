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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stores common metadata and binding validation for concrete metric specs.
 *
 * @param <M> the runtime metric type
 * @author RollW
 */
abstract class AbstractMetricSpec<M extends Metric> implements MetricSpec<M> {
    private final MetricName name;
    private final MetricKind<M> kind;
    private final List<MetricDimensionSpec<?>> dimensions;
    private final @Nullable MetricDescription description;
    private final @Nullable MetricUnit baseUnit;
    private final @Nullable MetricCardinalityPolicy cardinalityPolicy;

    AbstractMetricSpec(
            MetricName name,
            MetricKind<M> kind,
            List<MetricDimensionSpec<?>> dimensions,
            @Nullable MetricDescription description,
            @Nullable MetricUnit baseUnit,
            @Nullable MetricCardinalityPolicy cardinalityPolicy
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.dimensions = List.copyOf(dimensions);
        this.description = description;
        this.baseUnit = baseUnit;
        this.cardinalityPolicy = cardinalityPolicy;
        validateDimensions(this.dimensions);
    }

    @Override
    public MetricName getName() {
        return name;
    }

    @Override
    public MetricKind<M> getKind() {
        return kind;
    }

    @Override
    public List<MetricDimensionSpec<?>> getDimensions() {
        return dimensions;
    }

    @Override
    public @Nullable MetricDescription getDescription() {
        return description;
    }

    @Override
    public @Nullable MetricUnit getBaseUnit() {
        return baseUnit;
    }

    @Override
    public @Nullable MetricCardinalityPolicy getCardinalityPolicy() {
        return cardinalityPolicy;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricSpec<?> spec) || !getClass().equals(other.getClass())) {
            return false;
        }
        return name.equals(spec.getName())
                && kind.equals(spec.getKind())
                && dimensions.equals(spec.getDimensions())
                && Objects.equals(description, spec.getDescription())
                && Objects.equals(baseUnit, spec.getBaseUnit())
                && Objects.equals(cardinalityPolicy, spec.getCardinalityPolicy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), name, kind, dimensions, description, baseUnit, cardinalityPolicy);
    }

    @Override
    public MetricBinding<M> bind(MetricDimensionSet dimensionSet) {
        Objects.requireNonNull(dimensionSet, "dimensions");
        Map<MetricDimensionSpec<?>, Object> values = new LinkedHashMap<>();
        Map<String, String> formatted = new LinkedHashMap<>();
        for (MetricDimensionSpec<?> dimension : dimensions) {
            Object value = dimensionSet.values().get(dimension);
            if (value == null) {
                throw new IllegalArgumentException("Missing metric dimension: " + dimension.name());
            }
            String formattedValue = format(dimension, value);
            values.put(dimension, value);
            formatted.put(dimension.name(), formattedValue);
        }
        for (MetricDimensionSpec<?> supplied : dimensionSet.values().keySet()) {
            if (!dimensions.contains(supplied)) {
                throw new IllegalArgumentException("Extra metric dimension: " + supplied.name());
            }
        }
        return new MetricBinding<>(this, values, formatted, null);
    }

    MetricBinding<M> bindValueSource(MetricDimensionSet dimensions, Object valueSource) {
        Objects.requireNonNull(valueSource, "valueSource");
        MetricBinding<M> binding = bind(dimensions);
        return new MetricBinding<>(this, binding.getValues(), binding.getDimensions(), valueSource);
    }

    private static String format(MetricDimensionSpec<?> dimension, Object value) {
        return formatUnchecked(dimension, value);
    }

    private static <T> String formatUnchecked(MetricDimensionSpec<T> dimension, Object value) {
        return dimension.format(dimension.valueType().cast(value));
    }

    private static void validateDimensions(List<MetricDimensionSpec<?>> dimensions) {
        List<String> names = new ArrayList<>();
        for (MetricDimensionSpec<?> dimension : dimensions) {
            Objects.requireNonNull(dimension, "dimension");
            if (names.contains(dimension.name())) {
                throw new IllegalArgumentException("Metric dimensions must have unique names: " + dimension.name());
            }
            names.add(dimension.name());
        }
    }

    static abstract class BuilderSupport<M extends Metric, B extends BuilderSupport<M, B>> {
        private final MetricName name;
        private final MetricKind<M> kind;
        private final List<MetricDimensionSpec<?>> dimensions = new ArrayList<>();
        private @Nullable MetricDescription description;
        private @Nullable MetricUnit baseUnit;
        private @Nullable MetricCardinalityPolicy cardinalityPolicy;

        BuilderSupport(String name, MetricKind<M> kind) {
            this.name = MetricName.of(name);
            this.kind = kind;
        }

        @SuppressWarnings("unchecked")
        public B description(String description) {
            this.description = MetricDescription.of(description);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B baseUnit(String unit) {
            this.baseUnit = MetricUnit.of(unit);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B baseUnit(MetricUnit unit) {
            this.baseUnit = Objects.requireNonNull(unit, "unit");
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B dimension(MetricDimensionSpec<?> dimension) {
            this.dimensions.add(Objects.requireNonNull(dimension, "dimension"));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B cardinality(MetricCardinalityPolicy cardinalityPolicy) {
            this.cardinalityPolicy = Objects.requireNonNull(cardinalityPolicy, "cardinalityPolicy");
            return (B) this;
        }

        MetricName name() {
            return name;
        }

        MetricKind<M> kind() {
            return kind;
        }

        List<MetricDimensionSpec<?>> dimensions() {
            return List.copyOf(dimensions);
        }

        @Nullable MetricDescription description() {
            return description;
        }

        @Nullable MetricUnit baseUnit() {
            return baseUnit;
        }

        @Nullable MetricCardinalityPolicy cardinalityPolicy() {
            return cardinalityPolicy;
        }
    }
}
