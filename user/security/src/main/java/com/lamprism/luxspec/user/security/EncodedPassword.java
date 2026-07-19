package com.lamprism.luxspec.user.security;

import java.util.Objects;

/**
 * Holds one opaque encoded password representation suitable for protected storage.
 *
 * @author RollW
 */
public final class EncodedPassword {
    /**
     * The largest representation accepted by the initial local-user schema.
     */
    public static final int MAXIMUM_LENGTH = 255;

    private final String value;

    /**
     * Creates one bounded non-blank encoded password representation.
     *
     * @param value the opaque storage representation
     */
    public EncodedPassword(String value) {
        this.value = requireValue(value);
    }

    /**
     * Returns the exact opaque representation for a protected storage or adapter boundary.
     *
     * @return the encoded password representation
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EncodedPassword[redacted]";
    }

    private static String requireValue(String candidate) {
        String nonNullValue = Objects.requireNonNull(candidate, "value");
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException("Encoded password must not be blank");
        }
        if (nonNullValue.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("Encoded password exceeds the storage limit");
        }
        return nonNullValue;
    }
}
