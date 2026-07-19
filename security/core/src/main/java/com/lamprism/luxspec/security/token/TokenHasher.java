package com.lamprism.luxspec.security.token;

/**
 * Produces a typed one-way digest of a sensitive token value.
 *
 * @param <T> the token value type
 * @author RollW
 */
@FunctionalInterface
public interface TokenHasher<T extends Token> {
    /**
     * Produces a one-way Digest for a sensitive Token.
     *
     * @param token the Token to hash
     * @return the typed Token Digest
     */
    TokenDigest<T> hash(T token);
}
