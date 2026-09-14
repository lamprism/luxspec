package com.lamprism.luxspec.config.definition;

import com.lamprism.luxspec.config.ConfigCodec;
import com.lamprism.luxspec.config.ConfigDescription;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.policy.ConfigPolicy;
import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.validation.ValidationException;
import com.lamprism.luxspec.validation.Validator;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class DefaultConfigSpec<T> implements ConfigSpec<T> {
    private final ConfigKey key;
    private final ConfigCodec<T> codec;
    private final ConfigDescription description;
    private final @Nullable T defaultValue;
    private final boolean sensitive;
    private final ConfigPolicy policy;
    private final Validator<T> validator;

    public DefaultConfigSpec(
            ConfigKey key,
            ConfigCodec<T> codec,
            ConfigDescription description,
            @Nullable T defaultValue,
            boolean sensitive,
            Validator<T> validator,
            ConfigPolicy policy
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.description = Objects.requireNonNull(description, "description");
        this.defaultValue = defaultValue;
        this.sensitive = sensitive;
        this.policy = Objects.requireNonNull(policy, "policy");
        this.validator = Objects.requireNonNull(validator, "validator");
        if (defaultValue != null) {
            validate(defaultValue);
        }
    }

    @Override
    public ConfigKey getKey() {
        return key;
    }

    @Override
    public ConfigCodec<T> getCodec() {
        return codec;
    }

    @Override
    public ConfigDescription getDescription() {
        return description;
    }

    @Override
    public @Nullable T getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isSensitive() {
        return sensitive;
    }

    @Override
    public ConfigPolicy getPolicy() {
        return policy;
    }

    @Override
    public void validate(T value) {
        try {
            validator.validate(Objects.requireNonNull(value, "value"));
        } catch (ConfigValueValidationException exception) {
            throw exception;
        } catch (ValidationException exception) {
            throw new ConfigValueValidationException("Configuration value failed validation");
        }
    }
}
