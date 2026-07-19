package com.lamprism.luxspec.security.token;

/**
 * Represents one client-held sensitive token value.
 *
 * @author RollW
 */
public interface Token {
    /**
     * Returns the functional kind of this token.
     *
     * @return the token kind
     */
    TokenKind<? extends Token> getKind();

    /**
     * Returns the opaque token value.
     *
     * @return the sensitive token value
     */
    String getValue();
}
