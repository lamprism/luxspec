package com.lamprism.luxspec.security.token;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds a typed immutable one-way token digest.
 *
 * @param <T> the source token value type
 * @author RollW
 */
public final class TokenDigest<T extends Token> {
    private final TokenKind<T> tokenKind;
    private final byte[] value;

    /**
     * Creates a typed Digest with a defensive copy of its bytes.
     *
     * @param tokenKind the source Token kind
     * @param value the non-empty Digest bytes
     */
    public TokenDigest(TokenKind<T> tokenKind, byte[] value) {
        this.tokenKind = Objects.requireNonNull(tokenKind, "tokenKind");
        this.value = Objects.requireNonNull(value, "value").clone();
        if (this.value.length == 0) {
            throw new IllegalArgumentException("Token digest must not be empty");
        }
    }

    /**
     * Returns the source Token kind.
     *
     * @return the Token kind
     */
    public TokenKind<T> getTokenKind() {
        return tokenKind;
    }

    /**
     * Returns a defensive copy of the Digest bytes.
     *
     * @return the Digest bytes
     */
    public byte[] getValue() {
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TokenDigest<?> digest)) {
            return false;
        }
        return tokenKind.equals(digest.tokenKind) && Arrays.equals(value, digest.value);
    }

    @Override
    public int hashCode() {
        return 31 * tokenKind.hashCode() + Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "TokenDigest[kind=" + tokenKind.getName() + "]";
    }
}
