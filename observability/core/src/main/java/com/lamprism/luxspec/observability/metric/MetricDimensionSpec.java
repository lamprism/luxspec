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

import com.lamprism.luxspec.validation.ValidationRules;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Declares one typed, formatted metric dimension.
 *
 * <p>Dimension names are trimmed and reject empty or ISO control characters.
 * A builder requires an explicit formatter. Bound values must match the
 * declared type, produce a non-empty formatted value without control
 * characters, and fit within the configured maximum length of 256 characters
 * by default. Allowed values must not collide after formatting.
 *
 * @author RollW
 */
public final class MetricDimensionSpec<T> {
    private final String name;
    private final Class<T> valueType;
    private final Function<? super T, @Nullable String> formatter;
    private final List<T> allowedValues;
    private final int maximumValueLength;

    private MetricDimensionSpec(Builder<T> builder) {
        this.name = builder.name;
        this.valueType = builder.valueType;
        this.formatter = builder.formatter;
        this.allowedValues = List.copyOf(builder.allowedValues);
        this.maximumValueLength = builder.maximumValueLength;
        validateAllowedValues();
    }

    /**
     * Starts a generic typed dimension definition.
     *
     * @param name      the dimension name
     * @param valueType the value type
     * @param <T>       the value type
     * @return the builder
     */
    public static <T> Builder<T> builder(String name, Class<T> valueType) {
        return new Builder<>(name, valueType);
    }

    /**
     * Starts a String dimension definition.
     *
     * @param name the dimension name
     * @return the builder
     */
    public static Builder<String> string(String name) {
        return builder(name, String.class).formatter(Function.identity());
    }

    /**
     * Creates a String dimension with a fixed allowed domain.
     *
     * @param name   the dimension name
     * @param values the allowed values
     * @return the dimension
     */
    public static MetricDimensionSpec<String> allowed(String name, String... values) {
        return string(name).allowedValues(values).build();
    }

    /**
     * Creates a required String dimension without a cardinality domain.
     *
     * @param name the dimension name
     * @return the dimension
     */
    public static MetricDimensionSpec<String> required(String name) {
        return string(name).build();
    }

    public String name() {
        return name;
    }

    public Class<T> valueType() {
        return valueType;
    }

    public List<T> allowedValues() {
        return allowedValues;
    }

    public int maximumValueLength() {
        return maximumValueLength;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricDimensionSpec<?> spec)) {
            return false;
        }
        return name.equals(spec.name)
                && valueType.equals(spec.valueType)
                && allowedValues.equals(spec.allowedValues)
                && maximumValueLength == spec.maximumValueLength
                && formatter.getClass().equals(spec.formatter.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueType, allowedValues, maximumValueLength, formatter.getClass());
    }

    String format(T value) {
        Objects.requireNonNull(value, "value");
        if (!valueType.isInstance(value)) {
            throw new IllegalArgumentException("Metric dimension value does not match type: " + name);
        }
        if (!allowedValues.isEmpty() && !allowedValues.contains(value)) {
            throw new IllegalArgumentException("Metric dimension value is not allowed: " + name);
        }
        String formatted;
        try {
            formatted = formatter.apply(value);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Metric dimension formatter failed: " + name, failure);
        }
        if (formatted == null || formatted.isEmpty()) {
            throw new IllegalArgumentException("Metric dimension formatter returned an empty value: " + name);
        }
        if (formatted.length() > maximumValueLength) {
            throw new IllegalArgumentException("Metric dimension value exceeds maximum length: " + name);
        }
        ValidationRules.noControlCharacters("Metric dimension value").validate(formatted);
        return formatted;
    }

    private void validateAllowedValues() {
        List<String> formattedValues = new ArrayList<>();
        for (T value : allowedValues) {
            String formatted = format(value);
            if (formattedValues.contains(formatted)) {
                throw new IllegalArgumentException("Metric dimension allowed values have a formatted collision: " + name);
            }
            formattedValues.add(formatted);
        }
    }

    /**
     * Builds an immutable dimension declaration.
     */
    public static final class Builder<T> {
        private final String name;
        private final Class<T> valueType;
        private Function<? super T, @Nullable String> formatter;
        private final List<T> allowedValues = new ArrayList<>();
        private int maximumValueLength = 256;

        private Builder(String name, Class<T> valueType) {
            this.name = NameValidation.require(name, "Metric dimension name");
            this.valueType = Objects.requireNonNull(valueType, "valueType");
            if (valueType.isPrimitive()) {
                throw new IllegalArgumentException("Metric dimension value types must use boxed classes");
            }
        }

        public Builder<T> formatter(Function<? super T, @Nullable String> formatter) {
            this.formatter = Objects.requireNonNull(formatter, "formatter");
            return this;
        }

        public Builder<T> allowedValues(Collection<? extends T> values) {
            Objects.requireNonNull(values, "values");
            if (values.isEmpty()) {
                throw new IllegalArgumentException("allowedValues must not be empty");
            }
            allowedValues.clear();
            for (T value : values) {
                allowedValues.add(Objects.requireNonNull(value, "allowed value"));
            }
            return this;
        }

        @SafeVarargs
        public final Builder<T> allowedValues(T... values) {
            Objects.requireNonNull(values, "values");
            return allowedValues(List.of(values));
        }

        public Builder<T> maxValueLength(int maximumValueLength) {
            if (maximumValueLength < 1) {
                throw new IllegalArgumentException("maximumValueLength must be positive");
            }
            this.maximumValueLength = maximumValueLength;
            return this;
        }

        public MetricDimensionSpec<T> build() {
            if (formatter == null) {
                throw new IllegalStateException("A metric dimension formatter is required");
            }
            return new MetricDimensionSpec<>(this);
        }
    }
}
