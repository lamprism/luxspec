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
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatorRegistryTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-09T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void publishesSuccessFailureAndUnsupportedCredentialEvents() {
        List<AuthenticationEvent> events = new ArrayList<>();
        TestAuthenticator authenticator = new TestAuthenticator();
        AuthenticatorRegistry registry = new AuthenticatorRegistry(
                List.of(authenticator),
                event -> events.add((AuthenticationEvent) event),
                CLOCK
        );

        registry.authenticate(new TestCredentials());
        authenticator.setFailing(true);
        assertThrows(
                AuthenticationException.class,
                () -> registry.authenticate(new TestCredentials())
        );
        assertThrows(
                AuthenticationException.class,
                () -> registry.authenticate(new UnregisteredCredentials())
        );

        assertEquals(3, events.size());
        assertTrue(events.get(0).isSuccessful());
        assertFalse(events.get(1).isSuccessful());
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, events.get(1).getErrorCode());
        assertFalse(events.get(2).isSuccessful());
        assertEquals(AuthErrorCode.UNSUPPORTED_CREDENTIALS, events.get(2).getErrorCode());
        assertEquals("unsupported", events.get(2).getCredentialType());
    }

    private static final class TestCredentials implements Credentials {
    }

    private static final class UnregisteredCredentials implements Credentials {
    }

    private static final class TestAuthenticator implements Authenticator<TestCredentials> {
        private static final CredentialType<TestCredentials> TYPE = CredentialType.of(
                "test",
                TestCredentials.class
        );
        private boolean failing;

        @Override
        public CredentialType<TestCredentials> getCredentialType() {
            return TYPE;
        }

        @Override
        public Authentication authenticate(TestCredentials credentials) {
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
