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
import com.lamprism.luxspec.event.FailureHandlingEventPublisher;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class AuthenticationDispatcherTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-09T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void publishesSuccessFailureAndUnsupportedCredentialEvents() {
        List<AuthenticationEvent> events = new ArrayList<>();
        TestAuthenticator authenticator = new TestAuthenticator();
        AuthenticationDispatcher dispatcher = new AuthenticationDispatcherImpl(
                List.of(authenticator),
                event -> events.add((AuthenticationEvent) event),
                CLOCK
        );

        dispatcher.authenticate(new TestCredential());
        authenticator.setFailing(true);
        assertThrows(
                AuthenticationException.class,
                () -> dispatcher.authenticate(new TestCredential())
        );
        assertThrows(
                AuthenticationException.class,
                () -> dispatcher.authenticate(new UnregisteredCredential())
        );

        assertEquals(3, events.size());
        assertTrue(events.get(0).isSuccessful());
        assertFalse(events.get(1).isSuccessful());
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, events.get(1).getErrorCode());
        assertFalse(events.get(2).isSuccessful());
        assertEquals(AuthErrorCode.UNSUPPORTED_CREDENTIALS, events.get(2).getErrorCode());
        assertEquals("unsupported", events.get(2).getCredentialType());
    }

    @Test
    void doesNotPublishFailureAfterSuccessfulEventPublicationFails() {
        AtomicInteger publications = new AtomicInteger();
        AuthenticationDispatcher dispatcher = new AuthenticationDispatcherImpl(
                List.of(new TestAuthenticator()),
                event -> {
                    publications.incrementAndGet();
                    throw new IllegalStateException("Event publication failed");
                },
                CLOCK
        );

        assertThrows(IllegalStateException.class, () -> dispatcher.authenticate(new TestCredential()));

        assertEquals(1, publications.get());
    }

    @Test
    void preservesAuthenticationRejectionWhenEventPublicationFails() {
        IllegalStateException publicationFailure = new IllegalStateException("Event publication failed");
        TestAuthenticator authenticator = new TestAuthenticator();
        authenticator.setFailing(true);
        AuthenticationDispatcher dispatcher = new AuthenticationDispatcherImpl(
                List.of(authenticator), event -> {
            throw publicationFailure;
        }, CLOCK
        );

        AuthenticationException failure = assertThrows(
                AuthenticationException.class, () -> dispatcher.authenticate(new TestCredential())
        );
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, failure.getErrorCode());
        assertSame(publicationFailure, failure.getSuppressed()[0]);

        AuthenticationException unsupported = assertThrows(
                AuthenticationException.class, () -> dispatcher.authenticate(new UnregisteredCredential())
        );
        assertEquals(AuthErrorCode.UNSUPPORTED_CREDENTIALS, unsupported.getErrorCode());
        assertSame(publicationFailure, unsupported.getSuppressed()[0]);
    }

    @Test
    void permitsAnExplicitReportAndContinuePublicationPolicy() {
        IllegalStateException publicationFailure = new IllegalStateException("Event publication failed");
        AtomicInteger reported = new AtomicInteger();
        FailureHandlingEventPublisher publisher = new FailureHandlingEventPublisher(
                event -> {
                    throw publicationFailure;
                },
                (event, failure) -> {
                    assertSame(publicationFailure, failure);
                    reported.incrementAndGet();
                }
        );
        AuthenticationDispatcher dispatcher = new AuthenticationDispatcherImpl(
                List.of(new TestAuthenticator()), publisher, CLOCK
        );

        Authentication authentication = dispatcher.authenticate(new TestCredential());

        assertEquals("7", authentication.subject().getId());
        assertEquals(1, reported.get());
    }

    @Test
    void keepsPublicationFailureWhenTheSelectedFailureHandlerFails() {
        IllegalStateException publicationFailure = new IllegalStateException("Publication failed");
        IllegalStateException handlerFailure = new IllegalStateException("Failure handler failed");
        FailureHandlingEventPublisher publisher = new FailureHandlingEventPublisher(
                event -> {
                    throw publicationFailure;
                },
                (event, failure) -> {
                    throw handlerFailure;
                }
        );
        AuthenticationDispatcher dispatcher = new AuthenticationDispatcherImpl(
                List.of(new TestAuthenticator()), publisher, CLOCK
        );

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class, () -> dispatcher.authenticate(new TestCredential())
        );

        assertSame(handlerFailure, thrown);
        assertSame(publicationFailure, thrown.getSuppressed()[0]);
    }

    private static final class TestCredential implements Credential {
    }

    private static final class UnregisteredCredential implements Credential {
    }

    private static final class TestAuthenticator implements Authenticator<TestCredential> {
        private static final CredentialType<TestCredential> TYPE = CredentialType.of(
                "test",
                TestCredential.class
        );
        private boolean failing;

        @Override
        public CredentialType<TestCredential> getCredentialType() {
            return TYPE;
        }

        @Override
        public Authentication authenticate(TestCredential credentials) {
            if (failing) {
                throw new AuthenticationException(
                        AuthErrorCode.INVALID_CREDENTIALS,
                        "Test credentials were rejected"
                );
            }
            return new Authentication(
                    new UserSubject(7L),
                    AuthorizationGrantSet.of(List.of())
            );
        }

        private void setFailing(boolean failing) {
            this.failing = failing;
        }
    }
}
