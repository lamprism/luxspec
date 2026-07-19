package com.lamprism.luxspec.security.token;

/**
 * Identifies one trusted provider-neutral token verification result.
 *
 * @param <T> the verified token value type
 * @author RollW
 */
public interface TokenVerification<T extends Token> {
    /**
     * Returns the kind of Token represented by this trusted result.
     *
     * @return the verified Token kind
     */
    TokenKind<T> getTokenKind();
}
