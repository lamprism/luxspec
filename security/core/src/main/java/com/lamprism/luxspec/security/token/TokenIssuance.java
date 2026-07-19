package com.lamprism.luxspec.security.token;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Provides typed read-only access to tokens returned by one issue or refresh operation.
 *
 * @author RollW
 */
public interface TokenIssuance {
    /**
     * Creates an immutable token issuance.
     *
     * @param tokens the non-empty issued tokens
     * @return the immutable token issuance
     */
    static TokenIssuance of(Collection<? extends IssuedToken<? extends Token>> tokens) {
        return new ImmutableTokenIssuance(tokens);
    }

    /**
     * Finds an issued token by its functional kind.
     *
     * @param kind the requested token kind
     * @param <T> the token value type
     * @return the issued token when present
     */
    <T extends Token> Optional<IssuedToken<T>> find(TokenKind<T> kind);

    /**
     * Requires an issued token of one functional kind.
     *
     * @param kind the required token kind
     * @param <T> the token value type
     * @return the issued token
     * @throws IllegalArgumentException when that kind was not issued
     */
    <T extends Token> IssuedToken<T> require(TokenKind<T> kind);

    /**
     * Returns all issued tokens in stable issuance order.
     *
     * @return the immutable issued-token collection
     */
    Collection<IssuedToken<? extends Token>> getTokens();

    /**
     * Returns the immutable set of issued token kinds.
     *
     * @return the issued token kinds
     */
    Set<TokenKind<?>> getKinds();
}
