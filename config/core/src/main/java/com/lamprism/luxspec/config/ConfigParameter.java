package com.lamprism.luxspec.config;

import java.util.Objects;
import java.util.Set;

/**
 * Defines validation rules for one parameterized configuration path segment.
 *
 * @author RollW
 */
public final class ConfigParameter {
    private final String name;
    private final Set<String> allowedValues;

    public ConfigParameter(String name, Set<String> allowedValues) {
        this.name = requireName(name);
        this.allowedValues = Set.copyOf(allowedValues);
    }

    public String getName() {
        return name;
    }

    public void validate(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty() || value.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Parameter value must be one non-empty path segment");
        }
        ConfigKey.of("parameter." + value);
        if (!allowedValues.isEmpty() && !allowedValues.contains(value)) {
            throw new IllegalArgumentException("Parameter value is not allowed: " + name);
        }
    }

    private static String requireName(String value) {
        Objects.requireNonNull(value, "name");
        ConfigKey.of("parameter." + value);
        return value;
    }
}
