package com.lamprism.luxspec.security.token;

import java.util.Objects;

/**
 * Identifies one functional token kind and its exact Java value type.
 *
 * @param <T> the token value type
 * @author RollW
 */
public final class TokenKind<T extends Token> {
    private final String name;
    private final Class<T> tokenClass;

    private TokenKind(String name, Class<T> tokenClass) {
        this.name = requireName(name);
        this.tokenClass = Objects.requireNonNull(tokenClass, "tokenClass");
    }

    /**
     * Creates a typed token kind.
     *
     * @param name the canonical lower-kebab-case name
     * @param tokenClass the exact token value class
     * @param <T> the token value type
     * @return the token kind
     */
    public static <T extends Token> TokenKind<T> of(String name, Class<T> tokenClass) {
        return new TokenKind<>(name, tokenClass);
    }

    /**
     * Returns the canonical functional name.
     *
     * @return the token kind name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the exact Java token type.
     *
     * @return the token class
     */
    public Class<T> getTokenClass() {
        return tokenClass;
    }

    /**
     * Reports whether a token belongs to this kind.
     *
     * @param token the token to inspect
     * @return whether the token matches the exact kind and Java type
     */
    public boolean matches(Token token) {
        Token nonNullToken = Objects.requireNonNull(token, "token");
        return equals(nonNullToken.getKind()) && tokenClass.isInstance(nonNullToken);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TokenKind<?> tokenKind)) {
            return false;
        }
        return name.equals(tokenKind.name) && tokenClass.equals(tokenKind.tokenClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tokenClass);
    }

    @Override
    public String toString() {
        return name;
    }

    private static String requireName(String name) {
        String nonNullName = Objects.requireNonNull(name, "name");
        if (nonNullName.isBlank()) {
            throw new IllegalArgumentException("Token kind name must not be blank");
        }
        int segmentLength = 0;
        for (int index = 0; index < nonNullName.length(); index++) {
            char character = nonNullName.charAt(index);
            if (character >= 'a' && character <= 'z' || character >= '0' && character <= '9') {
                segmentLength++;
                continue;
            }
            if (character == '-' && segmentLength > 0) {
                segmentLength = 0;
                continue;
            }
            throw new IllegalArgumentException("Token kind name must use lower-kebab-case ASCII characters");
        }
        if (segmentLength == 0) {
            throw new IllegalArgumentException("Token kind name must not end with a separator");
        }
        return nonNullName;
    }
}
