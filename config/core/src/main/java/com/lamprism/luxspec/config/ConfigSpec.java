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

import com.lamprism.luxspec.config.policy.ConfigPolicies;
import com.lamprism.luxspec.config.policy.ConfigPolicy;
import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.validation.Validator;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        return ConfigPolicies.anySource();
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
}
