package com.lamprism.luxspec.security.token;

/**
 * Verifies a token and returns a trusted provider-neutral result.
 *
 * @param <T> the token value type
 * @param <V> the verification result type
 * @author RollW
 */
@FunctionalInterface
public interface TokenVerifier<T extends Token, V extends TokenVerification<T>> {
    /**
     * Verifies a Token and returns trusted provider-neutral data.
     *
     * @param token the untrusted presented Token
     * @return the trusted verification result
     */
    V verify(T token);
}
