package com.lamprism.luxspec.config.definition;

import com.lamprism.luxspec.config.ConfigCodec;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValueValidator;
import com.lamprism.luxspec.config.policy.ConfigPolicy;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class DefaultConfigSpec<T> implements ConfigSpec<T> {
    private final ConfigKey key;
    private final ConfigCodec<T> codec;
    private final @Nullable T defaultValue;
    private final boolean sensitive;
    private final ConfigPolicy policy;
    private final ConfigValueValidator<T> validator;

    public DefaultConfigSpec(
            ConfigKey key,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            ConfigValueValidator<T> validator,
            ConfigPolicy policy
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.codec = Objects.requireNonNull(codec, "codec");
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
        validator.validate(Objects.requireNonNull(value, "value"));
    }
}
