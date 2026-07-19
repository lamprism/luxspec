package com.lamprism.luxspec.security.token;

import java.time.Instant;

/**
 * Describes one newly issued token and its exact lifetime.
 *
 * @param <T> the token value type
 * @author RollW
 */
public interface IssuedToken<T extends Token> {
    /**
     * Creates a validated immutable issued-token value.
     *
     * @param token the newly issued token
     * @param issuedAt the issuance time
     * @param expiresAt the exclusive expiration time
     * @param <T> the token value type
     * @return the immutable issued token
     */
    static <T extends Token> IssuedToken<T> of(T token, Instant issuedAt, Instant expiresAt) {
        return new ImmutableIssuedToken<>(token, issuedAt, expiresAt);
    }

    /**
     * Returns the issued sensitive token value.
     *
     * @return the issued token
     */
    T getToken();

    /**
     * Returns the issuance time.
     *
     * @return the issuance time
     */
    Instant getIssuedAt();

    /**
     * Returns the exclusive expiration time.
     *
     * @return the expiration time
     */
    Instant getExpiresAt();
}
