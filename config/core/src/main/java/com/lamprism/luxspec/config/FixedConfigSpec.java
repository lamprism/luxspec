package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Defines one fixed complete configuration key.
 *
 * @param <T> the typed value
 * @author RollW
 */
public final class FixedConfigSpec<T> implements ConfigSpec<T> {
    private final ConfigKey key;
    private final ConfigCodec<T> codec;
    private final Optional<T> defaultValue;
    private final boolean sensitive;
    private final Set<ConfigSourceCapability> requiredSourceCapabilities;
    private final Map<String, String> requiredSourceAttributes;
    private final Optional<ConfigSourceId> requiredSourceId;

    private FixedConfigSpec(
            ConfigKey key,
            ConfigCodec<T> codec,
            Optional<T> defaultValue,
            boolean sensitive,
            Set<ConfigSourceCapability> requiredSourceCapabilities,
            Map<String, String> requiredSourceAttributes,
            @Nullable ConfigSourceId requiredSourceId
    ) {
        this.key = key;
        this.codec = codec;
        this.defaultValue = defaultValue;
        this.sensitive = sensitive;
        this.requiredSourceCapabilities = requiredSourceCapabilities;
        this.requiredSourceAttributes = requiredSourceAttributes;
        this.requiredSourceId = Optional.ofNullable(requiredSourceId);
    }

    public static <T> FixedConfigSpec<T> of(
            ConfigKey key,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive
    ) {
        return of(key, codec, defaultValue, sensitive, defaultCapabilities(sensitive), Map.of(), null);
    }

    /**
     * Creates a fixed definition with explicit usable-source requirements.
     *
     * @param key the complete configuration key
     * @param codec the typed codec
     * @param defaultValue the optional fallback value
     * @param sensitive whether values must be hidden from ordinary diagnostics
     * @param requiredSourceCapabilities required source capabilities
     * @param <T> the typed value
     * @return the fixed configuration definition
     */
    public static <T> FixedConfigSpec<T> of(
            ConfigKey key,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Set<ConfigSourceCapability> requiredSourceCapabilities
    ) {
        return of(key, codec, defaultValue, sensitive, requiredSourceCapabilities, Map.of(), null);
    }

    /**
     * Creates a fixed definition with complete source requirements.
     *
     * @param key the complete configuration key
     * @param codec the typed codec
     * @param defaultValue the optional fallback value
     * @param sensitive whether values must be hidden from ordinary diagnostics
     * @param requiredSourceCapabilities required source capabilities
     * @param requiredSourceAttributes required source attributes and values
     * @param requiredSourceId the optional exact source ID
     * @param <T> the typed value
     * @return the fixed configuration definition
     */
    public static <T> FixedConfigSpec<T> of(
            ConfigKey key,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Set<ConfigSourceCapability> requiredSourceCapabilities,
            Map<String, String> requiredSourceAttributes,
            @Nullable ConfigSourceId requiredSourceId
    ) {
        return new FixedConfigSpec<>(
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(codec, "codec"),
                Optional.ofNullable(defaultValue),
                sensitive,
                validateCapabilities(sensitive, requiredSourceCapabilities),
                ConfigSourceConstraints.validateAttributes(requiredSourceAttributes),
                requiredSourceId
        );
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
    public Optional<T> getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isSensitive() {
        return sensitive;
    }

    @Override
    public Set<ConfigSourceCapability> getRequiredSourceCapabilities() {
        return requiredSourceCapabilities;
    }

    @Override
    public Map<String, String> getRequiredSourceAttributes() {
        return requiredSourceAttributes;
    }

    @Override
    public Optional<ConfigSourceId> getRequiredSourceId() {
        return requiredSourceId;
    }

    private static Set<ConfigSourceCapability> defaultCapabilities(boolean sensitive) {
        if (sensitive) {
            return Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.SECURE);
        }
        return Set.of(ConfigSourceCapability.READ);
    }

    private static Set<ConfigSourceCapability> validateCapabilities(
            boolean sensitive,
            Set<ConfigSourceCapability> capabilities
    ) {
        Set<ConfigSourceCapability> values = Set.copyOf(Objects.requireNonNull(capabilities, "requiredSourceCapabilities"));
        if (!values.contains(ConfigSourceCapability.READ)) {
            throw new IllegalArgumentException("Configuration source requirements must include READ");
        }
        if (sensitive && !values.contains(ConfigSourceCapability.SECURE)) {
            throw new IllegalArgumentException("Sensitive configuration requires a secure source");
        }
        return values;
    }
}
