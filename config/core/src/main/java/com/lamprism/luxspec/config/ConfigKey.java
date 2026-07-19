package com.lamprism.luxspec.config;

import java.util.Objects;

/**
 * A validated complete configuration key.
 *
 * @author RollW
 */
public final class ConfigKey {
    private final String value;

    private ConfigKey(String value) {
        this.value = value;
    }

    public static ConfigKey of(String value) {
        Objects.requireNonNull(value, "value");
        validate(value);
        return new ConfigKey(value);
    }

    public String getValue() {
        return value;
    }

    private static void validate(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }
        boolean segmentStart = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '.') {
                if (segmentStart) {
                    throw new IllegalArgumentException("value contains an empty segment");
                }
                segmentStart = true;
                continue;
            }
            if (!isAllowed(character)) {
                throw new IllegalArgumentException("value contains an unsupported character");
            }
            segmentStart = false;
        }
        if (segmentStart) {
            throw new IllegalArgumentException("value must not end with a separator");
        }
    }

    private static boolean isAllowed(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || character == '-'
                || character == '_';
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConfigKey key && value.equals(key.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
