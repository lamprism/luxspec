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

import com.lamprism.luxspec.config.definition.DefaultConfigSpec;
import com.lamprism.luxspec.config.policy.ConfigPolicies;
import com.lamprism.luxspec.config.policy.ConfigPolicy;
import com.lamprism.luxspec.message.LocalizedText;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for fixed and parameterized configuration definitions.
 *
 * <p>The builder only collects definition inputs. Final key, parameter, sensitivity, default, and
 * validation checks remain in the single {@link DefaultConfigSpec} construction path.</p>
 *
 * @param <T> the typed value
 * @author RollW
 */
public final class ConfigSpecBuilder<T> {
    private final String key;
    private final ConfigCodec<T> codec;
    private ConfigDescription description = ConfigDescription.EMPTY;
    private List<ConfigParameter> parameters = List.of();
    private @Nullable T defaultValue;
    private boolean sensitive;
    private ConfigValueValidator<T> validator = ConfigValueValidator.none();
    private @Nullable ConfigPolicy policy;

    private ConfigSpecBuilder(String key, ConfigCodec<T> codec) {
        this.key = Objects.requireNonNull(key, "key");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /**
     * Creates a builder for one fixed or parameterized key expression.
     *
     * @param key   the key expression
     * @param codec the typed codec
     * @param <T>   the typed value
     * @return the definition builder
     */
    public static <T> ConfigSpecBuilder<T> builder(String key, ConfigCodec<T> codec) {
        return new ConfigSpecBuilder<>(key, codec);
    }

    /**
     * Replaces the parameter declarations.
     *
     * @param parameters the parameter declarations
     * @return this builder
     */
    public ConfigSpecBuilder<T> parameters(List<? extends ConfigParameter> parameters) {
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        return this;
    }

    /**
     * Sets the description metadata.
     *
     * @param description the optional localized description
     * @return this builder
     */
    public ConfigSpecBuilder<T> description(ConfigDescription description) {
        this.description = Objects.requireNonNull(description, "description");
        return this;
    }

    /**
     * Sets an inline description.
     *
     * @param description the description text
     * @return this builder
     */
    public ConfigSpecBuilder<T> textDescription(String description) {
        return description(ConfigDescription.text(description));
    }

    /**
     * Sets a locally translated description.
     *
     * @param description the local description text
     * @return this builder
     */
    public ConfigSpecBuilder<T> localizedDescription(LocalizedText description) {
        return description(ConfigDescription.localized(description));
    }

    /**
     * Adds one parameter declaration.
     *
     * @param parameter the parameter declaration
     * @return this builder
     */
    public ConfigSpecBuilder<T> parameter(ConfigParameter parameter) {
        List<ConfigParameter> values = new ArrayList<>(parameters);
        values.add(Objects.requireNonNull(parameter, "parameter"));
        parameters = List.copyOf(values);
        return this;
    }

    /**
     * Sets the optional default value.
     *
     * @param defaultValue the default value, or {@code null} for no default
     * @return this builder
     */
    public ConfigSpecBuilder<T> defaultValue(@Nullable T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Marks the definition as sensitive or non-sensitive.
     *
     * @param sensitive whether the definition contains sensitive data that must be redacted from
     *                  ordinary diagnostics; it does not configure encrypted storage or source
     *                  access control
     * @return this builder
     */
    public ConfigSpecBuilder<T> sensitive(boolean sensitive) {
        this.sensitive = sensitive;
        return this;
    }

    /**
     * Marks the definition as sensitive for disclosure and diagnostic redaction.
     *
     * <p>This does not select encrypted storage, secret-store routing, or source-level access
     * control.</p>
     *
     * @return this builder
     */
    public ConfigSpecBuilder<T> sensitive() {
        return sensitive(true);
    }

    /**
     * Sets the typed value validator.
     *
     * @param validator the value validator
     * @return this builder
     */
    public ConfigSpecBuilder<T> validator(ConfigValueValidator<T> validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
        return this;
    }

    /**
     * Adds an operation-aware policy to the definition.
     *
     * @param policy the policy to evaluate
     * @return this builder
     */
    public ConfigSpecBuilder<T> policy(ConfigPolicy policy) {
        ConfigPolicy nonNullPolicy = Objects.requireNonNull(policy, "policy");
        this.policy = this.policy == null
                ? nonNullPolicy
                : ConfigPolicies.allOf(this.policy, nonNullPolicy);
        return this;
    }

    /**
     * Builds the immutable configuration definition.
     *
     * @return the configuration definition
     */
    public ConfigSpec<T> build() {
        ConfigPolicy sourceSelection = ConfigPolicies.anySource();
        ConfigPolicy effectivePolicy = policy == null
                ? sourceSelection
                : ConfigPolicies.allOf(sourceSelection, policy);
        return new DefaultConfigSpec<>(
                ConfigKey.template(key, parameters),
                codec,
                description,
                defaultValue,
                sensitive,
                validator,
                effectivePolicy
        );
    }
}
