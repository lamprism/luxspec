package com.lamprism.luxspec.security.authentication;


/**
 * Authenticates one exact credential type.
 *
 * @param <C> the credential type
 * @author RollW
 */
public interface Authenticator<C extends Credentials> {
    /**
     * Returns the exact credential type this authenticator owns.
     *
     * @return the credential type
     */
    CredentialType<C> getCredentialType();

    /**
     * Authenticates one credential instance or throws a stable authentication failure.
     *
     * @param credentials the short-lived credential input
     * @return the authenticated subject and effective grants
     */
    Authentication authenticate(C credentials);
}
