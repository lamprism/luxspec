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

package com.lamprism.luxspec.console;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable typed positional argument declaration.
 *
 * @author RollW
 */
public final class ArgumentSpec<T> {
    private final String name;
    private final ValueParser<T> parser;
    private final @Nullable String valueLabel;
    private final @Nullable String description;
    private final @Nullable List<T> defaultValues;
    private final int minimumValues;
    private final int maximumValues;
    private final boolean hidden;

    private ArgumentSpec(String name,
                         ValueParser<T> parser,
                         @Nullable String valueLabel,
                         @Nullable String description,
                         @Nullable List<T> defaultValues,
                         int minimumValues,
                         int maximumValues,
                         boolean hidden) {
        this.name = validateName(name);
        this.parser = Objects.requireNonNull(parser, "parser");
        this.valueLabel = normalizeOptional(valueLabel, "Argument value label");
        this.description = normalizeOptional(description, "Argument description");
        this.defaultValues = defaultValues == null ? null : List.copyOf(defaultValues);
        this.minimumValues = minimumValues;
        this.maximumValues = maximumValues;
        this.hidden = hidden;
        validateConfiguration();
    }

    /**
     * Creates an argument builder with one required value.
     *
     * @param name   argument name
     * @param parser parser for one argument value
     * @param <T>    argument value type
     * @return the argument builder
     */
    public static <T> Builder<T> builder(String name, ValueParser<T> parser) {
        return new Builder<>(name, parser);
    }

    /**
     * @return the semantic argument name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the parser for one argument value
     */
    public ValueParser<T> getParser() {
        return parser;
    }

    /**
     * @return the optional Help value label
     */
    public @Nullable String getValueLabel() {
        return valueLabel;
    }

    /**
     * @return the optional Help description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * @return the minimum number of values
     */
    public int getMinimumValues() {
        return minimumValues;
    }

    /**
     * @return the maximum number of values
     */
    public int getMaximumValues() {
        return maximumValues;
    }

    /**
     * @return whether at least one value is required
     */
    public boolean isRequired() {
        return minimumValues > 0;
    }

    /**
     * @return whether more than one value may be supplied
     */
    public boolean isRepeatable() {
        return maximumValues > 1;
    }

    /**
     * @return whether this argument is hidden from default Help
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return whether an explicit default exists
     */
    public boolean hasDefaultValue() {
        return defaultValues != null;
    }

    /**
     * @return immutable typed default values, or an empty list when absent
     */
    public List<T> getDefaultValues() {
        if (defaultValues == null) {
            return List.of();
        }
        return defaultValues;
    }

    @Nullable
    List<T> defaultValuesOrNull() {
        return defaultValues;
    }

    /**
     * Builder for immutable positional arguments.
     */
    public static final class Builder<T> {
        private final String name;
        private final ValueParser<T> parser;
        private @Nullable String valueLabel;
        private @Nullable String description;
        private @Nullable List<T> defaultValues;
        private int minimumValues = 1;
        private int maximumValues = 1;
        private boolean hidden;

        private Builder(String name, ValueParser<T> parser) {
            this.name = Objects.requireNonNull(name, "name");
            this.parser = Objects.requireNonNull(parser, "parser");
        }

        /**
         * Sets the Help value label.
         *
         * @param valueLabel optional label used in Help output
         * @return this builder
         */
        public Builder<T> valueLabel(@Nullable String valueLabel) {
            this.valueLabel = valueLabel;
            return this;
        }

        /**
         * Sets the Help description.
         *
         * @param description optional description used in Help output
         * @return this builder
         */
        public Builder<T> description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Makes this argument optional.
         *
         * @return this builder
         */
        public Builder<T> optional() {
            this.minimumValues = 0;
            return this;
        }

        /**
         * Allows an unbounded number of values.
         *
         * @return this builder
         */
        public Builder<T> repeatable() {
            this.maximumValues = Integer.MAX_VALUE;
            return this;
        }

        /**
         * Sets an explicit minimum and maximum value count.
         *
         * @param minimumValues minimum number of values
         * @param maximumValues maximum number of values
         * @return this builder
         */
        public Builder<T> arity(int minimumValues, int maximumValues) {
            if (minimumValues < 0 || maximumValues < minimumValues || maximumValues == 0) {
                throw new IllegalArgumentException("Invalid argument arity: "
                        + minimumValues + ".." + maximumValues);
            }
            this.minimumValues = minimumValues;
            this.maximumValues = maximumValues;
            return this;
        }

        /**
         * Hides this argument from default Help.
         *
         * @return this builder
         */
        public Builder<T> hidden() {
            this.hidden = true;
            return this;
        }

        /**
         * Sets one typed default value.
         *
         * @param value default value
         * @return this builder
         */
        public Builder<T> defaultValue(T value) {
            this.defaultValues = List.of(Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Sets typed defaults for an optional repeatable argument.
         *
         * @param values non-empty typed default values
         * @return this builder
         */
        public Builder<T> defaultValues(List<? extends T> values) {
            Objects.requireNonNull(values, "values");
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Argument default values cannot be empty");
            }
            this.defaultValues = new ArrayList<>(values.size());
            for (T value : values) {
                this.defaultValues.add(Objects.requireNonNull(value, "values cannot contain null"));
            }
            return this;
        }

        /**
         * Builds the immutable argument.
         *
         * @return the immutable argument specification
         */
        public ArgumentSpec<T> build() {
            return new ArgumentSpec<>(name, parser, valueLabel, description, defaultValues,
                    minimumValues, maximumValues, hidden);
        }
    }

    private void validateConfiguration() {
        if (minimumValues < 0 || maximumValues < minimumValues || maximumValues == 0) {
            throw new IllegalArgumentException("Invalid argument arity: "
                    + minimumValues + ".." + maximumValues);
        }
        if (defaultValues != null) {
            if (minimumValues > 0) {
                throw new IllegalArgumentException("A required argument cannot define a default value");
            }
            if (defaultValues.size() > maximumValues) {
                throw new IllegalArgumentException("Argument defaults exceed the maximum arity");
            }
        }
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Argument name cannot be blank");
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character)) {
                throw new IllegalArgumentException("Argument name cannot contain whitespace: " + name);
            }
        }
        if (name.indexOf('=') >= 0 || name.charAt(0) == '-') {
            throw new IllegalArgumentException("Invalid argument name: " + name);
        }
        return name;
    }

    private static @Nullable String normalizeOptional(@Nullable String value, String name) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}
