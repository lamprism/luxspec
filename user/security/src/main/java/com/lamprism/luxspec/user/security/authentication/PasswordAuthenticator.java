/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.user.security.authentication;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.resource.ResourceException;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationEvent;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Authenticator;
import com.lamprism.luxspec.security.authentication.CredentialType;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.resource.UserProvider;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolver;
import com.lamprism.luxspec.user.security.password.EncodedPassword;
import com.lamprism.luxspec.user.security.password.PasswordScheme;
import com.lamprism.luxspec.user.security.password.UserPasswordStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a password authenticator from user lookup, password storage, protection, and grant roles.
     *
     * @param userProvider   the authoritative user lookup role
     * @param passwordStore  the protected password persistence boundary
     * @param passwordScheme the authoritative password protection scheme
     * @param grantResolver  the user-role grant resolver
     */
    public PasswordAuthenticator(
            UserProvider userProvider,
            UserPasswordStore passwordStore,
            PasswordScheme passwordScheme,
            UserRoleGrantResolver grantResolver
    ) {
        this(userProvider, passwordStore, passwordScheme, grantResolver, event -> {
        }, Clock.systemUTC());
    }

    /**
     * Creates a password authenticator with explicit security event publication.
     *
     * @param userProvider   the authoritative user lookup role
     * @param passwordStore  the protected password persistence boundary
     * @param passwordScheme the authoritative password protection scheme
     * @param grantResolver  the user-role grant resolver
     * @param eventPublisher the authentication event publisher
     * @param clock          the authentication event timestamp clock
     */
    public PasswordAuthenticator(
            UserProvider userProvider,
            UserPasswordStore passwordStore,
            PasswordScheme passwordScheme,
            UserRoleGrantResolver grantResolver,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.userProvider = Objects.requireNonNull(userProvider, "userProvider");
        this.passwordStore = Objects.requireNonNull(passwordStore, "passwordStore");
        this.passwordScheme = Objects.requireNonNull(passwordScheme, "passwordScheme");
        this.grantResolver = Objects.requireNonNull(grantResolver, "grantResolver");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
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
        Instant startedAt = clock.instant();
        Authentication authentication;
        try {
            authentication = authenticateInternal(credentials);
        } catch (AuthenticationException failure) {
            Instant completedAt = clock.instant();
            eventPublisher.publish(AuthenticationEvent.failed(
                    CREDENTIAL_TYPE.getName(),
                    failure.getErrorCode(),
                    completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
            throw failure;
        } catch (RuntimeException failure) {
            Instant completedAt = clock.instant();
            eventPublisher.publish(AuthenticationEvent.failed(
                    CREDENTIAL_TYPE.getName(),
                    AuthErrorCode.AUTHENTICATION_FAILURE,
                    completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
            throw failure;
        }
        Instant completedAt = clock.instant();
        eventPublisher.publish(AuthenticationEvent.succeeded(
                CREDENTIAL_TYPE.getName(),
                authentication,
                completedAt,
                elapsedSince(startedAt, completedAt)
        ));
        return authentication;
    }

    private Authentication authenticateInternal(UsernamePasswordCredentials credentials) {
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

    private static Duration elapsedSince(Instant startedAt, Instant completedAt) {
        Duration elapsed = Duration.between(startedAt, completedAt);
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
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
