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

package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.event.EventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dispatches credentials to one authoritative authenticator by exact Java type.
 *
 * <p>Authentication events should be published at one boundary. Applications that pass the same
 * publisher to this registry and to a registered authenticator will receive duplicate attempt
 * events.</p>
 *
 * @author RollW
 */
public final class AuthenticatorRegistry {
    private static final String UNSUPPORTED_CREDENTIAL_TYPE = "unsupported";
    private final Map<Class<? extends Credentials>, Authenticator<?>> authenticators;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates immutable exact-type authenticator registrations.
     *
     * @param authenticators the authoritative authenticators
     */
    public AuthenticatorRegistry(Iterable<? extends Authenticator<?>> authenticators) {
        this(authenticators, event -> {
        }, Clock.systemUTC());
    }

    /**
     * Creates immutable exact-type registrations with authentication event publication.
     *
     * @param authenticators the authoritative authenticators
     * @param eventPublisher the authentication event publisher
     */
    public AuthenticatorRegistry(
            Iterable<? extends Authenticator<?>> authenticators,
            EventPublisher eventPublisher
    ) {
        this(authenticators, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates immutable exact-type registrations with explicit event timing.
     *
     * @param authenticators the authoritative authenticators
     * @param eventPublisher the authentication event publisher
     * @param clock          the authentication event timestamp clock
     */
    public AuthenticatorRegistry(
            Iterable<? extends Authenticator<?>> authenticators,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        Map<Class<? extends Credentials>, Authenticator<?>> registrations = new HashMap<>();
        Map<String, Class<? extends Credentials>> names = new HashMap<>();
        for (Authenticator<?> authenticator : authenticators) {
            register(registrations, names, Objects.requireNonNull(authenticator, "authenticator"));
        }
        this.authenticators = Map.copyOf(registrations);
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
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
        Instant startedAt = clock.instant();
        Authenticator<?> authenticator = authenticators.get(credentials.getClass());
        if (authenticator == null) {
            AuthenticationException failure = new AuthenticationException(
                    AuthErrorCode.UNSUPPORTED_CREDENTIALS,
                    "No authenticator is registered for the credential type"
            );
            Instant completedAt = clock.instant();
            eventPublisher.publish(AuthenticationEvent.failed(
                    UNSUPPORTED_CREDENTIAL_TYPE,
                    failure.getErrorCode(),
                    completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
            throw failure;
        }
        String credentialType = authenticator.getCredentialType().getName();
        Authentication authentication;
        try {
            authentication = authenticate(authenticator, credentials);
        } catch (AuthenticationException failure) {
            Instant completedAt = clock.instant();
            eventPublisher.publish(AuthenticationEvent.failed(
                    credentialType,
                    failure.getErrorCode(),
                    completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
            throw failure;
        } catch (RuntimeException failure) {
            Instant completedAt = clock.instant();
            eventPublisher.publish(AuthenticationEvent.failed(
                    credentialType,
                    AuthErrorCode.AUTHENTICATION_FAILURE,
                    completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
            throw failure;
        }
        Instant completedAt = clock.instant();
        eventPublisher.publish(AuthenticationEvent.succeeded(
                credentialType,
                authentication,
                completedAt,
                elapsedSince(startedAt, completedAt)
        ));
        return authentication;
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

    private static Duration elapsedSince(Instant startedAt, Instant completedAt) {
        Duration elapsed = Duration.between(startedAt, completedAt);
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }
}
