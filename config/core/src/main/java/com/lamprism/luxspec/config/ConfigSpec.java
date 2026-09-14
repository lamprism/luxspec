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

package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.policy.ConfigPolicy;
import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.validation.Validator;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Defines one typed configuration value independent of its storage provider.
 *
 * <p>The same definition model represents fixed and parameterized key expressions. Callers use one
 * definition API and do not select a separate template type.</p>
 *
 * @param <T> the typed value
 * @author RollW
 */
public interface ConfigSpec<T> {
    /**
     * Creates a fluent builder for one fixed or parameterized definition.
     *
     * @param key   the key expression
     * @param codec the typed codec
     * @param <T>   the typed value
     * @return the definition builder
     */
    static <T> ConfigSpecBuilder<T> builder(String key, ConfigCodec<T> codec) {
        return ConfigSpecBuilder.builder(key, codec);
    }

    /**
     * Creates a validator that rejects blank text in a configuration value.
     *
     * @return the non-blank text validator
     */
    static Validator<String> nonBlankText() {
        return validator(
                value -> !value.isBlank(),
                "Text value must not be blank"
        );
    }

    /**
     * Creates an inclusive range validator for comparable configuration values.
     *
     * @param minimum the inclusive lower bound
     * @param maximum the inclusive upper bound
     * @param <T>     the comparable value type
     * @return the range validator
     */
    static <T extends Comparable<? super T>> Validator<T> betweenInclusive(T minimum, T maximum) {
        T nonNullMinimum = Objects.requireNonNull(minimum, "minimum");
        T nonNullMaximum = Objects.requireNonNull(maximum, "maximum");
        if (nonNullMinimum.compareTo(nonNullMaximum) > 0) {
            throw new IllegalArgumentException("minimum must not be greater than maximum");
        }
        return validator(
                value -> value.compareTo(nonNullMinimum) >= 0
                        && value.compareTo(nonNullMaximum) <= 0,
                "Value is outside the configured range"
        );
    }

    /**
     * Creates a validator that accepts strictly positive numbers.
     *
     * @param <T> the numeric value type
     * @return the positive-number validator
     */
    static <T extends Number> Validator<T> positiveNumber() {
        return validator(
                value -> compareWithZero(value) > 0,
                "Numeric value must be positive"
        );
    }

    /**
     * Creates a validator that accepts zero and positive numbers.
     *
     * @param <T> the numeric value type
     * @return the non-negative-number validator
     */
    static <T extends Number> Validator<T> nonNegativeNumber() {
        return validator(
                value -> compareWithZero(value) >= 0,
                "Numeric value must not be negative"
        );
    }

    /**
     * Creates a validator that rejects non-finite floating-point values.
     *
     * @param <T> the numeric value type
     * @return the finite-number validator
     */
    static <T extends Number> Validator<T> finiteNumber() {
        return validator(
                ConfigSpec::isFinite,
                "Numeric value must be finite"
        );
    }

    /**
     * Creates a validator that accepts only members of a fixed set.
     *
     * @param allowedValues the non-empty allowed value set
     * @param <T>           the value type
     * @return the membership validator
     */
    static <T> Validator<T> oneOf(Set<? extends T> allowedValues) {
        Set<? extends T> values = Set.copyOf(Objects.requireNonNull(allowedValues, "allowedValues"));
        if (values.isEmpty()) {
            throw new IllegalArgumentException("allowedValues must not be empty");
        }
        return validator(
                values::contains,
                "Value is not an allowed member"
        );
    }

    /**
     * Creates a validator that applies one validator to every list element.
     *
     * @param elementValidator the element validator
     * @param <T>              the element type
     * @return the list element validator
     */
    static <T> Validator<List<T>> elements(Validator<? super T> elementValidator) {
        Validator<? super T> nonNullValidator = Objects.requireNonNull(elementValidator, "elementValidator");
        return values -> {
            List<T> nonNullValues = Objects.requireNonNull(values, "value");
            for (T value : nonNullValues) {
                nonNullValidator.validate(value);
            }
        };
    }

    /**
     * Creates a fixed definition with the default Source-selection policy.
     *
     * @param key          the complete key without parameters
     * @param codec        the typed codec
     * @param defaultValue the fallback value, or {@code null} when no fallback exists
     * @param sensitive    whether values are sensitive disclosure metadata and must remain hidden
     *                     from ordinary diagnostics; this does not select encrypted storage or
     *                     enforce source access control
     * @param validator    the typed value rules
     * @param <T>          the typed value
     * @return the configuration definition
     */
    static <T> ConfigSpec<T> of(
            String key,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Validator<T> validator
    ) {
        return builder(key, codec)
                .defaultValue(defaultValue)
                .sensitive(sensitive)
                .validator(validator)
                .build();
    }

    /**
     * Creates a fixed definition with no additional value rules.
     *
     * @param key          the complete key without parameters
     * @param codec        the typed codec
     * @param defaultValue the fallback value, or {@code null} when no fallback exists
     * @param sensitive    whether values are sensitive disclosure metadata and must remain hidden
     *                     from ordinary diagnostics; this does not select encrypted storage or
     *                     enforce source access control
     * @param <T>          the typed value
     * @return the configuration definition
     */
    static <T> ConfigSpec<T> of(
            String key,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive
    ) {
        return of(key, codec, defaultValue, sensitive, Validator.none());
    }

    /**
     * Creates a parameterized definition with the default Source-selection policy.
     *
     * @param key          the fixed or parameterized key expression
     * @param parameters   declarations for parameter segments in the key
     * @param codec        the typed codec
     * @param defaultValue the fallback value, or {@code null} when no fallback exists
     * @param sensitive    whether values are sensitive disclosure metadata and must remain hidden
     *                     from ordinary diagnostics; this does not select encrypted storage or
     *                     enforce source access control
     * @param validator    the typed value rules
     * @param <T>          the typed value
     * @return the configuration definition
     */
    static <T> ConfigSpec<T> of(
            String key,
            List<ConfigParameter> parameters,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Validator<T> validator
    ) {
        return builder(key, codec)
                .parameters(parameters)
                .defaultValue(defaultValue)
                .sensitive(sensitive)
                .validator(validator)
                .build();
    }

    /**
     * Creates a parameterized definition with no additional value rules.
     *
     * @param key          the fixed or parameterized key expression
     * @param parameters   declarations for parameter segments in the key
     * @param codec        the typed codec
     * @param defaultValue the fallback value, or {@code null} when no fallback exists
     * @param sensitive    whether values are sensitive disclosure metadata and must remain hidden
     *                     from ordinary diagnostics; this does not select encrypted storage or
     *                     enforce source access control
     * @param <T>          the typed value
     * @return the configuration definition
     */
    static <T> ConfigSpec<T> of(
            String key,
            List<ConfigParameter> parameters,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive
    ) {
        return of(key, parameters, codec, defaultValue, sensitive, Validator.none());
    }

    /**
     * Creates a fixed definition with an explicit policy and value rules.
     *
     * @param key          the complete key without parameters
     * @param codec        the typed codec
     * @param defaultValue the fallback value, or {@code null} when no fallback exists
     * @param sensitive    whether values are sensitive disclosure metadata and must remain hidden
     *                     from ordinary diagnostics; this does not select encrypted storage or
     *                     enforce source access control
     * @param validator    the typed value rules
     * @param policy       the operation-aware policy
     * @param <T>          the typed value
     * @return the configuration definition
     */
    static <T> ConfigSpec<T> of(
            String key,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Validator<T> validator,
            ConfigPolicy policy
    ) {
        return of(
                key,
                List.of(),
                codec,
                defaultValue,
                sensitive,
                validator,
                policy
        );
    }

    /**
     * Creates a definition with an explicit policy and value rules.
     *
     * @param key          the fixed or parameterized key expression
     * @param parameters   declarations for parameter segments in the key
     * @param codec        the typed codec
     * @param defaultValue the fallback value, or {@code null} when no fallback exists
     * @param sensitive    whether values are sensitive disclosure metadata and must remain hidden
     *                     from ordinary diagnostics; this does not select encrypted storage or
     *                     enforce source access control
     * @param validator    the typed value rules
     * @param policy       the operation-aware policy
     * @param <T>          the typed value
     * @return the configuration definition
     */
    static <T> ConfigSpec<T> of(
            String key,
            List<ConfigParameter> parameters,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Validator<T> validator,
            ConfigPolicy policy
    ) {
        return builder(key, codec)
                .parameters(parameters)
                .defaultValue(defaultValue)
                .sensitive(sensitive)
                .validator(validator)
                .policy(policy)
                .build();
    }

    /**
     * Returns the fixed or parameterized key expression of this definition.
     *
     * @return the configuration key expression
     */
    ConfigKey getKey();

    /**
     * Returns the codec that converts this definition's raw value.
     *
     * @return the typed codec
     */
    ConfigCodec<T> getCodec();

    /**
     * Returns the optional localized description of this definition.
     *
     * @return the immutable description metadata
     */
    default ConfigDescription getDescription() {
        return ConfigDescription.EMPTY;
    }

    /**
     * Returns the fallback used when every usable source is absent.
     *
     * @return the default value, or {@code null} when no fallback exists
     */
    @Nullable T getDefaultValue();

    /**
     * Reports whether administrative views and events must not reveal its value.
     *
     * <p>This flag classifies disclosure and redaction behavior. It does not imply encryption at
     * rest, secret-store routing, or source-level access control.</p>
     *
     * @return {@code true} when the value is sensitive
     */
    boolean isSensitive();

    /**
     * Returns the operation-aware policy applied by the Provider runtime.
     *
     * @return the immutable configuration policy
     */
    default ConfigPolicy getPolicy() {
        return ConfigPolicy.anySource();
    }

    /**
     * Validates one non-null typed value against this definition's domain rules.
     *
     * @param value the typed value
     * @throws ConfigValueValidationException when the value violates the definition
     */
    void validate(T value);

    /**
     * Binds the definition to one complete key.
     *
     * @param arguments the values for every declared key parameter
     * @return the validated concrete binding
     */
    default ConfigBinding<T> bind(Map<String, String> arguments) {
        return new ConfigBinding<>(this, Objects.requireNonNull(arguments, "arguments"));
    }

    /**
     * Binds a fixed definition without arguments.
     *
     * @return the validated concrete binding
     */
    default ConfigBinding<T> bind() {
        return bind(Map.of());
    }

    /**
     * Matches a complete key during catalog reverse lookup.
     *
     * @param key the complete configuration key
     * @return the matching binding, or {@code null} when this definition does not match
     */
    @Nullable
    default ConfigBinding<T> match(ConfigKey key) {
        Map<String, String> arguments = getKey().match(Objects.requireNonNull(key, "key"));
        if (arguments == null) {
            return null;
        }
        return bind(arguments);
    }

    /**
     * Reports whether a complete key belongs to this definition.
     *
     * <p>This is the lightweight validation operation for discovery and administration code. It
     * does not create a {@link ConfigBinding}.</p>
     *
     * @param key the complete configuration key
     * @return {@code true} when this definition matches the key
     */
    default boolean matches(ConfigKey key) {
        return getKey().match(Objects.requireNonNull(key, "key")) != null;
    }

    private static <T> Validator<T> validator(Predicate<? super T> predicate, String detail) {
        Predicate<? super T> nonNullPredicate = Objects.requireNonNull(predicate, "predicate");
        String nonBlankDetail = Objects.requireNonNull(detail, "detail");
        if (nonBlankDetail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return value -> {
            T nonNullValue = Objects.requireNonNull(value, "value");
            if (!nonNullPredicate.test(nonNullValue)) {
                throw new ConfigValueValidationException(nonBlankDetail);
            }
        };
    }

    private static int compareWithZero(Number value) {
        Number nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue instanceof BigDecimal decimal) {
            return decimal.compareTo(BigDecimal.ZERO);
        }
        if (nonNullValue instanceof BigInteger integer) {
            return integer.signum();
        }
        if (nonNullValue instanceof Byte
                || nonNullValue instanceof Short
                || nonNullValue instanceof Integer
                || nonNullValue instanceof Long) {
            return Long.compare(nonNullValue.longValue(), 0L);
        }
        if (nonNullValue instanceof Float || nonNullValue instanceof Double) {
            double numericValue = nonNullValue.doubleValue();
            if (Double.isNaN(numericValue)) {
                return -2;
            }
            return numericValue < 0 ? -1 : numericValue > 0 ? 1 : 0;
        }
        throw new IllegalArgumentException("Unsupported numeric value type");
    }

    private static boolean isFinite(Number value) {
        Number nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue instanceof Float floatValue) {
            return Float.isFinite(floatValue);
        }
        if (nonNullValue instanceof Double doubleValue) {
            return Double.isFinite(doubleValue);
        }
        if (nonNullValue instanceof BigDecimal || nonNullValue instanceof BigInteger
                || nonNullValue instanceof Byte || nonNullValue instanceof Short
                || nonNullValue instanceof Integer || nonNullValue instanceof Long) {
            return true;
        }
        throw new IllegalArgumentException("Unsupported numeric value type");
    }
}
