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
 * Immutable typed option declaration.
 *
 * @author RollW
 */
public final class OptionSpec<T> {
    private final List<String> names;
    private final ValueParser<T> parser;
    private final @Nullable String valueLabel;
    private final @Nullable String description;
    private final @Nullable List<T> defaultValues;
    private final boolean flag;
    private final boolean required;
    private final boolean repeatable;
    private final boolean hidden;

    private OptionSpec(List<String> names,
                       ValueParser<T> parser,
                       @Nullable String valueLabel,
                       @Nullable String description,
                       @Nullable List<T> defaultValues,
                       boolean flag,
                       boolean required,
                       boolean repeatable,
                       boolean hidden) {
        this.names = copyNames(names);
        this.parser = Objects.requireNonNull(parser, "parser");
        this.valueLabel = normalizeOptional(valueLabel, "Option value label");
        this.description = normalizeOptional(description, "Option description");
        this.defaultValues = defaultValues == null ? null : List.copyOf(defaultValues);
        this.flag = flag;
        this.required = required;
        this.repeatable = repeatable;
        this.hidden = hidden;
        validateConfiguration();
    }

    /**
     * Creates a value-taking option builder.
     *
     * @param name   canonical option name
     * @param parser parser for one option value
     * @param <T>    option value type
     * @return the option builder
     */
    public static <T> Builder<T> builder(String name, ValueParser<T> parser) {
        return new Builder<>(name, parser, false);
    }

    /**
     * Creates a boolean presence flag builder.
     *
     * @param name canonical option name
     * @return the flag builder
     */
    public static Builder<Boolean> flag(String name) {
        return new Builder<>(name, ValueParsers.booleanValue(), true);
    }

    /**
     * @return the canonical name followed by ordered aliases
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * @return the canonical name
     */
    public String getName() {
        return names.get(0);
    }

    /**
     * @return the parser for one value
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
     * @return whether this option is a presence flag
     */
    public boolean isFlag() {
        return flag;
    }

    /**
     * @return whether the option must be supplied
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @return whether the option may occur more than once
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * @return whether the option is hidden from default Help
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
     * Builder for immutable typed options.
     */
    public static final class Builder<T> {
        private final List<String> names = new ArrayList<>();
        private final ValueParser<T> parser;
        private final boolean flag;
        private @Nullable String valueLabel;
        private @Nullable String description;
        private @Nullable List<T> defaultValues;
        private boolean required;
        private boolean repeatable;
        private boolean hidden;

        private Builder(String name, ValueParser<T> parser, boolean flag) {
            this.names.add(Objects.requireNonNull(name, "name"));
            this.parser = Objects.requireNonNull(parser, "parser");
            this.flag = flag;
        }

        /**
         * Adds one ordered alias.
         *
         * @param alias option alias
         * @return this builder
         */
        public Builder<T> alias(String alias) {
            names.add(Objects.requireNonNull(alias, "alias"));
            return this;
        }

        /**
         * Adds ordered aliases.
         *
         * @param aliases option aliases
         * @return this builder
         */
        public Builder<T> aliases(String... aliases) {
            Objects.requireNonNull(aliases, "aliases");
            for (String alias : aliases) {
                alias(alias);
            }
            return this;
        }

        /**
         * Sets the value label shown by Help.
         *
         * @param valueLabel optional label shown for the value
         * @return this builder
         */
        public Builder<T> valueLabel(@Nullable String valueLabel) {
            this.valueLabel = valueLabel;
            return this;
        }

        /**
         * Sets the Help description.
         *
         * @param description optional description shown in Help
         * @return this builder
         */
        public Builder<T> description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Requires one occurrence.
         *
         * @return this builder
         */
        public Builder<T> required() {
            this.required = true;
            return this;
        }

        /**
         * Allows the option to occur more than once.
         *
         * @return this builder
         */
        public Builder<T> repeatable() {
            this.repeatable = true;
            return this;
        }

        /**
         * Hides this option from the default Help document.
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
         * Sets typed defaults for a repeatable option.
         *
         * @param values non-empty typed default values
         * @return this builder
         */
        public Builder<T> defaultValues(List<? extends T> values) {
            Objects.requireNonNull(values, "values");
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Option default values cannot be empty");
            }
            this.defaultValues = new ArrayList<>(values.size());
            for (T value : values) {
                this.defaultValues.add(Objects.requireNonNull(value, "values cannot contain null"));
            }
            return this;
        }

        /**
         * Builds the immutable option.
         *
         * @return the immutable option specification
         */
        public OptionSpec<T> build() {
            return new OptionSpec<>(names, parser, valueLabel, description, defaultValues,
                    flag, required, repeatable, hidden);
        }
    }

    private void validateConfiguration() {
        if (flag && valueLabel != null) {
            throw new IllegalArgumentException("A flag cannot define a value label");
        }
        if (flag && defaultValues != null) {
            throw new IllegalArgumentException("A flag cannot define a default value");
        }
        if (required && defaultValues != null) {
            throw new IllegalArgumentException("A required option cannot define a default value");
        }
        if (defaultValues != null && !repeatable && defaultValues.size() != 1) {
            throw new IllegalArgumentException("A non-repeatable option requires one default value");
        }
    }

    private static List<String> copyNames(List<String> names) {
        Objects.requireNonNull(names, "names");
        if (names.isEmpty()) {
            throw new IllegalArgumentException("At least one option name is required");
        }
        List<String> copiedNames = new ArrayList<>(names.size());
        for (String name : names) {
            validateOptionName(name);
            if (copiedNames.contains(name)) {
                throw new IllegalArgumentException("Duplicate option name: " + name);
            }
            copiedNames.add(name);
        }
        return List.copyOf(copiedNames);
    }

    static void validateOptionName(String name) {
        Objects.requireNonNull(name, "option name");
        if (name.isBlank() || name.indexOf('=') >= 0 || containsWhitespace(name)) {
            throw new IllegalArgumentException("Invalid option name: " + name);
        }
        if (name.equals("-") || name.equals("--")) {
            throw new IllegalArgumentException("Invalid option name: " + name);
        }
        if (name.startsWith("--")) {
            if (name.length() == 2 || name.charAt(2) == '-') {
                throw new IllegalArgumentException("Invalid long option name: " + name);
            }
            return;
        }
        if (name.length() != 2 || name.charAt(0) != '-') {
            throw new IllegalArgumentException("Short option names must contain one character: " + name);
        }
    }

    private static boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character)) {
                return true;
            }
        }
        return false;
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
