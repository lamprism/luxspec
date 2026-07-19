package com.lamprism.luxspec.security.token.refresh;

import java.util.Objects;

/**
 * Identifies one stable Refresh Token Session.
 *
 * @author RollW
 */
public final class RefreshTokenSessionId {
    private final String value;

    /**
     * Creates a Refresh Token Session identifier.
     *
     * @param value the non-blank identifier value
     */
    public RefreshTokenSessionId(String value) {
        this.value = Objects.requireNonNull(value, "value");
        if (this.value.isBlank()) {
            throw new IllegalArgumentException("Refresh Token Session ID must not be blank");
        }
    }

    /**
     * Returns the stable identifier value.
     *
     * @return the identifier value
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RefreshTokenSessionId sessionId)) {
            return false;
        }
        return value.equals(sessionId.value);
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
