package com.lamprism.luxspec.config;

import java.util.Objects;

/**
 * Identifies one configured configuration source instance.
 *
 * @author RollW
 */
public final class ConfigSourceId {
    private final String value;

    private ConfigSourceId(String value) {
        this.value = value;
    }

    public static ConfigSourceId of(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        return new ConfigSourceId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConfigSourceId identifier && value.equals(identifier.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
