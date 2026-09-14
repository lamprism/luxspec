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
import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.event.EventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Routes credentials to exact-type authenticators and publishes one result event per attempt.
 *
 * @author RollW
 */
public class AuthenticationDispatcherImpl implements AuthenticationDispatcher {
    private static final String UNSUPPORTED_CREDENTIAL_TYPE = "unsupported";

    private final Map<Class<? extends Credential>, Authenticator<?>> authenticators;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates immutable exact-type registrations with explicit event timing.
     *
     * @param authenticators the authoritative authenticators
     * @param eventPublisher the authentication event publisher
     * @param clock          the authentication event timestamp clock
     */
    public AuthenticationDispatcherImpl(
            Iterable<? extends Authenticator<?>> authenticators,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        Map<Class<? extends Credential>, Authenticator<?>> registrations = new HashMap<>();
        Map<String, Class<? extends Credential>> names = new HashMap<>();
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
    @Override
    public Authentication authenticate(Credential credentials) {
        Credential nonNullCredential = Objects.requireNonNull(credentials, "credentials");
        Instant startedAt = clock.instant();
        Authenticator<?> authenticator = authenticators.get(nonNullCredential.getClass());
        if (authenticator == null) {
            AuthenticationException failure = new AuthenticationException(
                    AuthErrorCode.UNSUPPORTED_CREDENTIALS,
                    "No authenticator is registered for the credential type"
            );
            publishFailure(UNSUPPORTED_CREDENTIAL_TYPE, failure.getErrorCode(), startedAt);
            throw failure;
        }
        String credentialType = authenticator.getCredentialType().getName();
        Authentication authentication;
        try {
            authentication = authenticate(authenticator, nonNullCredential);
        } catch (AuthenticationException failure) {
            publishFailure(credentialType, failure.getErrorCode(), startedAt);
            throw failure;
        } catch (RuntimeException failure) {
            publishFailure(credentialType, AuthErrorCode.AUTHENTICATION_FAILURE, startedAt);
            throw failure;
        }
        publishSuccess(credentialType, authentication, startedAt);
        return authentication;
    }

    private void publishSuccess(String credentialType, Authentication authentication, Instant startedAt) {
        Instant completedAt = clock.instant();
        eventPublisher.publish(AuthenticationEvent.succeeded(
                credentialType,
                authentication,
                completedAt,
                elapsedSince(startedAt, completedAt)
        ));
    }

    private void publishFailure(String credentialType, ErrorCode errorCode, Instant startedAt) {
        Instant completedAt = clock.instant();
        eventPublisher.publish(AuthenticationEvent.failed(
                credentialType,
                errorCode,
                completedAt,
                elapsedSince(startedAt, completedAt)
        ));
    }

    private void register(
            Map<Class<? extends Credential>, Authenticator<?>> registrations,
            Map<String, Class<? extends Credential>> names,
            Authenticator<?> authenticator
    ) {
        CredentialType<?> credentialType = authenticator.getCredentialType();
        Class<? extends Credential> credentialClass = credentialType.getCredentialClass();
        Class<? extends Credential> previousType = names.putIfAbsent(credentialType.getName(), credentialClass);
        if (previousType != null && previousType != credentialClass) {
            throw new IllegalArgumentException("Credential type name is registered for a different Java type");
        }
        if (registrations.putIfAbsent(credentialClass, authenticator) != null) {
            throw new IllegalArgumentException("Multiple authenticators are registered for one credential type");
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends Credential> Authentication authenticate(
            Authenticator<C> authenticator,
            Credential credentials
    ) {
        return authenticator.authenticate((C) credentials);
    }

    private static Duration elapsedSince(Instant startedAt, Instant completedAt) {
        Duration elapsed = Duration.between(startedAt, completedAt);
        if (elapsed.isNegative()) {
            return Duration.ZERO;
        }
        return elapsed;
    }
}
