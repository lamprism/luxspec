package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.token.Token;
import com.lamprism.luxspec.security.token.TokenKind;
import java.util.Objects;

/**
 * Holds one opaque client-presented Refresh Token value.
 *
 * @author RollW
 */
public final class RefreshToken implements Token {
    /**
     * The standard Refresh Token kind.
     */
    public static final TokenKind<RefreshToken> KIND = TokenKind.of("refresh", RefreshToken.class);

    private final String value;

    /**
     * Creates a Refresh Token from an opaque sensitive value.
     *
     * @param value the non-blank token value
     */
    public RefreshToken(String value) {
        this.value = Objects.requireNonNull(value, "value");
        if (this.value.isBlank()) {
            throw new IllegalArgumentException("Refresh Token value must not be blank");
        }
    }

    @Override
    public TokenKind<RefreshToken> getKind() {
        return KIND;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "RefreshToken[REDACTED]";
    }
}
