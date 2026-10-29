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
import com.lamprism.luxspec.security.authorization.AuthorizationScopeHierarchy;
import com.lamprism.luxspec.security.token.CurrentTokenAuthenticationPolicy;
import com.lamprism.luxspec.security.token.TokenAuthenticationPolicy;
import com.lamprism.luxspec.security.token.TokenGrantPolicy;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.AccessTokenVerifier;
import com.lamprism.luxspec.security.token.access.EventPublishingAccessTokenRevocationStore;
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

/**
 *
 * @author RollW
 */
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
    void authenticatesWithAnExplicitRevocationStore() {
        TestSubjectResolver subjectResolver = new TestSubjectResolver();
        TrackingRevocationStore revocationStore = new TrackingRevocationStore();
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                TokenAuthenticationPolicy.issuedGrants(subjectResolver),
                revocationStore
        );

        Authentication authentication = authenticator.authenticate(credentials());

        assertEquals("user", authentication.subject().getType());
        assertEquals("42", authentication.subject().getId());
        assertEquals(VERIFIED_TOKEN.getGrants(), authentication.grants());
        assertEquals(1, subjectResolver.getInvocationCount());
    }

    @Test
    void rejectsARevokedTokenBeforeResolvingTheSubject() {
        TestSubjectResolver subjectResolver = new TestSubjectResolver();
        TrackingRevocationStore revocationStore = new TrackingRevocationStore();
        revocationStore.revoke(VERIFIED_TOKEN, VERIFIED_TOKEN.getExpiresAt());
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                TokenAuthenticationPolicy.issuedGrants(subjectResolver),
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
    void limitsIssuedGrantsToCurrentGrants() {
        AuthorizationGrantSet current = AuthorizationGrantSet.of(List.of(
                AuthorizationScope.of("account:read"),
                AuthorizationScope.of("account:write")
        ));
        CurrentTokenAuthenticationPolicy policy = new CurrentTokenAuthenticationPolicy(
                subject -> new Authentication(new UserSubject(42L), current)
        );
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(), policy, new TrackingRevocationStore()
        );

        assertEquals(VERIFIED_TOKEN.getGrants(), authenticator.authenticate(credentials()).grants());
        Authentication reduced = new CurrentTokenAuthenticationPolicy(
                subject -> new Authentication(new UserSubject(42L), AuthorizationGrantSet.of(List.of()))
        ).resolve(new UserSubject(42L), VERIFIED_TOKEN.getGrants());
        assertEquals(AuthorizationGrantSet.of(List.of()), reduced.grants());
    }

    @Test
    void permitsAnExplicitEmptyGrantPolicy() {
        TokenGrantPolicy rejectEmpty = TokenGrantPolicy.expandedIntersection(
                AuthorizationScopeHierarchy.empty(), true
        );
        TokenGrantPolicy allowEmpty = TokenGrantPolicy.expandedIntersection(
                AuthorizationScopeHierarchy.empty(), false
        );
        AuthorizationGrantSet empty = AuthorizationGrantSet.of(List.of());

        assertThrows(AuthenticationException.class, () -> rejectEmpty.apply(empty, VERIFIED_TOKEN.getGrants()));
        assertEquals(empty, allowEmpty.apply(empty, VERIFIED_TOKEN.getGrants()));
    }

    @Test
    void rejectsAPolicyThatReturnsAnotherSubject() {
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                (subject, grants) -> new Authentication(new UserSubject(43L), grants),
                new TrackingRevocationStore()
        );

        assertEquals(AuthErrorCode.INVALID_TOKEN, assertThrows(
                AuthenticationException.class,
                () -> authenticator.authenticate(credentials())
        ).getErrorCode());
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

        store.revoke(VERIFIED_TOKEN, VERIFIED_TOKEN.getExpiresAt());

        assertEquals(1, events.size());
        assertEquals(TokenLifecycleEvent.Operation.REVOKE, events.get(0).getOperation());
        assertEquals(TokenLifecycleEvent.Result.SUCCESS, events.get(0).getResult());
        assertEquals(List.of(AccessToken.KIND.getName()), List.copyOf(events.get(0).getTokenKinds()));
        assertNull(events.get(0).getSubject());
    }

    private static AccessTokenCredential credentials() {
        return new AccessTokenCredential(new AccessToken("encoded-access-token"));
    }

    private static final class TestTokenVerifier implements AccessTokenVerifier {
        @Override
        public VerifiedAccessToken verify(AccessToken accessToken) {
            Objects.requireNonNull(accessToken, "accessToken");
            return VERIFIED_TOKEN;
        }

        @Override
        public Instant getAcceptanceDeadline(VerifiedAccessToken accessToken) {
            return Objects.requireNonNull(accessToken, "accessToken").getExpiresAt();
        }
    }

    private static final class TestSubjectResolver implements SubjectResolver {
        private int invocationCount;

        @Override
        public Subject resolve(String type, String id) {
            if (!"user".equals(type)) {
                throw new IllegalArgumentException("Unexpected subject type");
            }
            invocationCount++;
            return new UserSubject(Long.parseLong(id));
        }

        private int getInvocationCount() {
            return invocationCount;
        }
    }

    private static final class TrackingRevocationStore implements AccessTokenRevocationStore {
        private String revokedTokenId;
        private int checkCount;

        @Override
        public void revoke(VerifiedAccessToken accessToken, Instant acceptanceDeadline) {
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
