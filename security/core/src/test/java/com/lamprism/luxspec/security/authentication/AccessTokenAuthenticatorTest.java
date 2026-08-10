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
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;
import com.lamprism.luxspec.security.token.TokenVerifier;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.EventPublishingAccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.NoOpAccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessTokenAuthenticatorTest {
    private static final VerifiedAccessToken VERIFIED_TOKEN = VerifiedAccessToken.of(
            "user",
            "42",
            AuthorizationGrantSet.of(List.of(AuthorizationScope.of("account:read"))),
            "access-token-id",
            Instant.parse("2026-07-12T00:00:00Z"),
            Instant.parse("2026-07-12T00:05:00Z")
    );

    @Test
    void authenticatesWithoutAnOptionalRevocationStore() {
        TestSubjectResolver subjectResolver = new TestSubjectResolver();
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                subjectResolver
        );

        Authentication authentication = authenticator.authenticate(credentials());

        assertEquals(new UserSubject(42L).getId(), authentication.subject().getId());
        assertEquals(VERIFIED_TOKEN.getGrants(), authentication.grants());
        assertEquals(1, subjectResolver.getInvocationCount());
    }

    @Test
    void rejectsARevokedTokenBeforeResolvingTheSubject() {
        TestSubjectResolver subjectResolver = new TestSubjectResolver();
        TrackingRevocationStore revocationStore = new TrackingRevocationStore();
        revocationStore.revoke(VERIFIED_TOKEN);
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                subjectResolver,
                revocationStore
        );

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authenticator.authenticate(credentials())
        );

        assertEquals(AuthErrorCode.ACCESS_TOKEN_REVOKED, exception.getErrorCode());
        assertEquals(1, revocationStore.getCheckCount());
        assertEquals(0, subjectResolver.getInvocationCount());
    }

    @Test
    void publishesSafeAuthenticationEvents() {
        List<AuthenticationEvent> events = new ArrayList<>();
        TestSubjectResolver subjectResolver = new TestSubjectResolver();
        AccessTokenAuthenticator successfulAuthenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                subjectResolver,
                NoOpAccessTokenRevocationStore.getInstance(),
                event -> events.add((AuthenticationEvent) event),
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC)
        );

        successfulAuthenticator.authenticate(credentials());

        TrackingRevocationStore revocationStore = new TrackingRevocationStore();
        revocationStore.revoke(VERIFIED_TOKEN);
        AccessTokenAuthenticator failedAuthenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                subjectResolver,
                revocationStore,
                event -> events.add((AuthenticationEvent) event),
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC)
        );
        assertThrows(AuthenticationException.class, () -> failedAuthenticator.authenticate(credentials()));

        assertEquals(2, events.size());
        assertEquals(true, events.get(0).isSuccessful());
        assertEquals("user", events.get(0).getSubject().getType());
        assertEquals(false, events.get(1).isSuccessful());
        assertEquals(AuthErrorCode.ACCESS_TOKEN_REVOKED, events.get(1).getErrorCode());
    }

    @Test
    void publishesAccessTokenRevocationWithoutTheTokenIdentifier() {
        List<TokenLifecycleEvent> events = new ArrayList<>();
        TrackingRevocationStore delegate = new TrackingRevocationStore();
        EventPublishingAccessTokenRevocationStore store = new EventPublishingAccessTokenRevocationStore(
                delegate,
                event -> events.add((TokenLifecycleEvent) event),
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC)
        );

        store.revoke(VERIFIED_TOKEN);

        assertEquals(1, events.size());
        assertEquals(TokenLifecycleEvent.Operation.REVOKE, events.get(0).getOperation());
        assertEquals(TokenLifecycleEvent.Result.SUCCESS, events.get(0).getResult());
        assertEquals(List.of(AccessToken.KIND.getName()), List.copyOf(events.get(0).getTokenKinds()));
        assertNull(events.get(0).getSubject());
    }

    private static AccessTokenCredentials credentials() {
        return new AccessTokenCredentials(new AccessToken("encoded-access-token"));
    }

    private static final class TestTokenVerifier
            implements TokenVerifier<AccessToken, VerifiedAccessToken> {
        @Override
        public VerifiedAccessToken verify(AccessToken accessToken) {
            Objects.requireNonNull(accessToken, "accessToken");
            return VERIFIED_TOKEN;
        }
    }

    private static final class TestSubjectResolver implements SubjectResolver {
        private int invocationCount;

        @Override
        public Subject resolve(String subjectType, String subjectId) {
            if (!"user".equals(subjectType)) {
                throw new IllegalArgumentException("Unexpected subject type");
            }
            invocationCount++;
            return new UserSubject(Long.parseLong(subjectId));
        }

        private int getInvocationCount() {
            return invocationCount;
        }
    }

    private static final class TrackingRevocationStore implements AccessTokenRevocationStore {
        private String revokedTokenId;
        private int checkCount;

        @Override
        public void revoke(VerifiedAccessToken accessToken) {
            revokedTokenId = Objects.requireNonNull(accessToken, "accessToken").getTokenId();
        }

        @Override
        public boolean isRevoked(VerifiedAccessToken accessToken) {
            checkCount++;
            return Objects.requireNonNull(accessToken, "accessToken").getTokenId().equals(revokedTokenId);
        }

        private int getCheckCount() {
            return checkCount;
        }
    }
}
