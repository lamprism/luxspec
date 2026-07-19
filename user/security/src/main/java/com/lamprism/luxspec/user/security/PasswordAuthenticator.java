package com.lamprism.luxspec.user.security;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.resource.ResourceException;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Authenticator;
import com.lamprism.luxspec.security.authentication.CredentialType;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserProvider;
import java.util.Objects;
import java.util.Optional;

/**
 * Authenticates username and password credentials without exposing persistence internals.
 *
 * @author RollW
 */
public final class PasswordAuthenticator implements Authenticator<UsernamePasswordCredentials> {
    private static final CredentialType<UsernamePasswordCredentials> CREDENTIAL_TYPE = CredentialType.of(
            "username-password",
            UsernamePasswordCredentials.class
    );

    private final UserProvider userProvider;
    private final UserPasswordStore passwordStore;
    private final PasswordScheme passwordScheme;
    private final UserRoleGrantResolver grantResolver;

    /**
     * Creates a password authenticator from user lookup, password storage, protection, and grant roles.
     *
     * @param userProvider the authoritative user lookup role
     * @param passwordStore the protected password persistence boundary
     * @param passwordScheme the authoritative password protection scheme
     * @param grantResolver the user-role grant resolver
     */
    public PasswordAuthenticator(
            UserProvider userProvider,
            UserPasswordStore passwordStore,
            PasswordScheme passwordScheme,
            UserRoleGrantResolver grantResolver
    ) {
        this.userProvider = Objects.requireNonNull(userProvider, "userProvider");
        this.passwordStore = Objects.requireNonNull(passwordStore, "passwordStore");
        this.passwordScheme = Objects.requireNonNull(passwordScheme, "passwordScheme");
        this.grantResolver = Objects.requireNonNull(grantResolver, "grantResolver");
    }

    /**
     * Returns the username and password credential type.
     *
     * @return the exact credential type handled by this authenticator
     */
    @Override
    public CredentialType<UsernamePasswordCredentials> getCredentialType() {
        return CREDENTIAL_TYPE;
    }

    /**
     * Authenticates a current active user and opportunistically upgrades a verified representation.
     *
     * @param credentials the short-lived username and password credentials
     * @return the authenticated user and resolved effective grants
     */
    @Override
    public Authentication authenticate(UsernamePasswordCredentials credentials) {
        UsernamePasswordCredentials nonNullCredentials = Objects.requireNonNull(credentials, "credentials");
        User user = findUser(nonNullCredentials.getUsername());
        UserSubject subject = UserSubjectResolver.requireActive(user);
        EncodedPassword encodedPassword = findPassword(user.id());
        if (!passwordScheme.verify(nonNullCredentials.getRawPassword(), encodedPassword)) {
            throw invalidCredentials();
        }
        upgradeIfNeeded(user.id(), encodedPassword, nonNullCredentials.getRawPassword());
        return new Authentication(subject, grantResolver.resolve(user.roles()));
    }

    private User findUser(String username) {
        try {
            return userProvider.provideByUsername(username);
        } catch (ResourceException exception) {
            throw invalidCredentials();
        }
    }

    private EncodedPassword findPassword(long userId) {
        Optional<EncodedPassword> password = passwordStore.find(userId);
        if (password.isEmpty()) {
            throw invalidCredentials();
        }
        return password.get();
    }

    private void upgradeIfNeeded(long userId, EncodedPassword currentPassword, CharSequence rawPassword) {
        if (passwordScheme.needsUpgrade(currentPassword)) {
            passwordStore.replace(userId, currentPassword, passwordScheme.encode(rawPassword));
        }
    }

    private static AuthenticationException invalidCredentials() {
        return new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS, "Username or password is invalid");
    }
}
