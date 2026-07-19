package com.lamprism.luxspec.security.token.access;

import com.lamprism.luxspec.security.token.Token;
import com.lamprism.luxspec.security.token.TokenKind;
import java.util.Objects;

/**
 * Holds one opaque client-presented Access Token value.
 *
 * @author RollW
 */
public final class AccessToken implements Token {
    /**
     * The standard Access Token kind.
     */
    public static final TokenKind<AccessToken> KIND = TokenKind.of("access", AccessToken.class);

    private final String value;

    /**
     * Creates an Access Token from an opaque sensitive value.
     *
     * @param value the non-blank token value
     */
    public AccessToken(String value) {
        this.value = Objects.requireNonNull(value, "value");
        if (this.value.isBlank()) {
            throw new IllegalArgumentException("Access Token value must not be blank");
        }
    }

    @Override
    public TokenKind<AccessToken> getKind() {
        return KIND;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "AccessToken[REDACTED]";
    }
}
