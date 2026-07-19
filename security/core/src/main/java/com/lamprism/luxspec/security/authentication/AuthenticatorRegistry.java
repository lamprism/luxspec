package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.AuthErrorCode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dispatches credentials to one authoritative authenticator by exact Java type.
 *
 * @author RollW
 */
public final class AuthenticatorRegistry {
    private final Map<Class<? extends Credentials>, Authenticator<?>> authenticators;

    /**
     * Creates immutable exact-type authenticator registrations.
     *
     * @param authenticators the authoritative authenticators
     */
    public AuthenticatorRegistry(Iterable<? extends Authenticator<?>> authenticators) {
        Map<Class<? extends Credentials>, Authenticator<?>> registrations = new HashMap<>();
        Map<String, Class<? extends Credentials>> names = new HashMap<>();
        for (Authenticator<?> authenticator : authenticators) {
            register(registrations, names, Objects.requireNonNull(authenticator, "authenticator"));
        }
        this.authenticators = Map.copyOf(registrations);
    }

    /**
     * Dispatches credentials to their exact registered authenticator.
     *
     * @param credentials the supplied typed credentials
     * @return the resulting authenticated actor
     * @throws AuthenticationException when no authenticator accepts the credential type
     */
    public Authentication authenticate(Credentials credentials) {
        Objects.requireNonNull(credentials, "credentials");
        Authenticator<?> authenticator = authenticators.get(credentials.getClass());
        if (authenticator == null) {
            throw new AuthenticationException(
                    AuthErrorCode.UNSUPPORTED_CREDENTIALS,
                    "No authenticator is registered for the credential type"
            );
        }
        return authenticate(authenticator, credentials);
    }

    private void register(
            Map<Class<? extends Credentials>, Authenticator<?>> registrations,
            Map<String, Class<? extends Credentials>> names,
            Authenticator<?> authenticator
    ) {
        CredentialType<?> credentialType = authenticator.getCredentialType();
        Class<? extends Credentials> credentialsType = credentialType.getCredentialsType();
        Class<? extends Credentials> previousType = names.putIfAbsent(credentialType.getName(), credentialsType);
        if (previousType != null && previousType != credentialsType) {
            throw new IllegalArgumentException("Credential type name is registered for a different Java type");
        }
        if (registrations.putIfAbsent(credentialsType, authenticator) != null) {
            throw new IllegalArgumentException("Multiple authenticators are registered for one credential type");
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends Credentials> Authentication authenticate(Authenticator<C> authenticator, Credentials credentials) {
        return authenticator.authenticate((C) credentials);
    }
}
