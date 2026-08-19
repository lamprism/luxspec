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

package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.token.IssuedToken;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenIssuance;
import com.lamprism.luxspec.security.token.TokenIssuer;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.refresh.support.StoredRefreshTokenLifecycle;
import com.lamprism.luxspec.security.token.support.Sha256TokenHasher;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoredRefreshTokenLifecycleTest {
    private static final Instant ISSUED_AT = Instant.parse("2026-07-15T00:00:00Z");
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration MAXIMUM_LIFETIME = Duration.ofMinutes(30);
    private static final AuthorizationScope READ_SCOPE = AuthorizationScope.of("account:read");
    private static final AuthorizationScope WRITE_SCOPE = AuthorizationScope.of("account:write");
    private static final AuthorizationScope NEW_SCOPE = AuthorizationScope.of("account:new");

    @Test
    void keepsRefreshCapabilityOptionalAtTheIssuerBoundary() {
        Fixture fixture = new Fixture();
        TokenIssuance accessOnly = fixture.accessTokenIssuer.issue(initialAuthentication());
        TokenIssuer refreshEnabledIssuer = fixture.lifecycle;

        TokenIssuance refreshEnabled = refreshEnabledIssuer.issue(initialAuthentication());

        assertEquals(List.of(AccessToken.KIND), List.copyOf(accessOnly.getKinds()));
        assertTrue(refreshEnabled.find(AccessToken.KIND).isPresent());
        assertTrue(refreshEnabled.find(RefreshToken.KIND).isPresent());
        assertEquals(1, fixture.store.sessionCount());
    }

    @Test
    void createsASessionAndStoresOnlyTheRefreshTokenDigest() {
        Fixture fixture = new Fixture();

        TokenIssuance issuance = fixture.lifecycle.issue(initialAuthentication());
        IssuedToken<RefreshToken> issuedRefreshToken = issuance.require(RefreshToken.KIND);
        TokenDigest<RefreshToken> digest = fixture.hasher.hash(issuedRefreshToken.getToken());
        RefreshTokenSessionId sessionId = fixture.store.getSessionId(digest);
        RefreshTokenSession session = fixture.store.getSession(sessionId);

        assertEquals(new UserSubject(42L).getType(), session.getSubject().getType());
        assertEquals(new UserSubject(42L).getId(), session.getSubject().getId());
        assertEquals(initialAuthentication().grants(), session.getAuthorizationCeiling());
        assertEquals(ISSUED_AT, session.getLifetime().getCreatedAt());
        assertEquals(ISSUED_AT.plus(IDLE_TIMEOUT), issuedRefreshToken.getExpiresAt());
        assertEquals(1, fixture.store.digestCount());
        assertFalse(digest.toString().contains(issuedRefreshToken.getToken().getValue()));
    }

    @Test
    void rotatesTokensAndReconcilesCurrentGrantsWithTheSessionCeiling() {
        Fixture fixture = new Fixture();
        TokenIssuance initialIssuance = fixture.lifecycle.issue(initialAuthentication());
        RefreshToken initialToken = initialIssuance.require(RefreshToken.KIND).getToken();
        TokenDigest<RefreshToken> initialDigest = fixture.hasher.hash(initialToken);
        fixture.authenticationResolver.setAuthentication(new Authentication(
                new UserSubject(42L),
                grants(READ_SCOPE, NEW_SCOPE)
        ));
        fixture.clock.setInstant(ISSUED_AT.plus(Duration.ofMinutes(1)));

        TokenIssuance successorIssuance = fixture.lifecycle.refresh(initialToken);
        RefreshToken successorToken = successorIssuance.require(RefreshToken.KIND).getToken();
        TokenDigest<RefreshToken> successorDigest = fixture.hasher.hash(successorToken);
        RefreshTokenSessionId sessionId = fixture.store.getSessionId(initialDigest);
        RefreshTokenSession session = fixture.store.getSession(sessionId);

        assertNotEquals(initialToken.getValue(), successorToken.getValue());
        assertTrue(successorIssuance.find(AccessToken.KIND).isPresent());
        assertEquals(grants(READ_SCOPE), fixture.accessTokenIssuer.getLastAuthentication().grants());
        assertEquals(sessionId, fixture.store.getSessionId(successorDigest));
        assertTrue(fixture.store.isConsumed(initialDigest));
        assertFalse(fixture.store.isConsumed(successorDigest));
        assertEquals(ISSUED_AT.plus(Duration.ofMinutes(1)), session.getLifetime().getLastActivityAt());
        assertEquals(ISSUED_AT.plus(Duration.ofMinutes(11)), session.getLifetime().getIdleExpiresAt());
    }

    @Test
    void rejectsRefreshAtTheIdleExpirationBoundary() {
        Fixture fixture = new Fixture();
        RefreshToken refreshToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        TokenDigest<RefreshToken> digest = fixture.hasher.hash(refreshToken);
        fixture.clock.setInstant(ISSUED_AT.plus(IDLE_TIMEOUT));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> fixture.lifecycle.refresh(refreshToken)
        );

        assertEquals(AuthErrorCode.REFRESH_TOKEN_REJECTED, exception.getErrorCode());
        assertEquals(1, fixture.store.digestCount());
        assertFalse(fixture.store.isConsumed(digest));
    }

    @Test
    void neverRenewsBeyondTheAbsoluteExpiration() {
        Fixture fixture = new Fixture(Duration.ofMinutes(20), MAXIMUM_LIFETIME);
        RefreshToken initialToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        TokenDigest<RefreshToken> initialDigest = fixture.hasher.hash(initialToken);
        fixture.clock.setInstant(ISSUED_AT.plus(Duration.ofMinutes(15)));

        RefreshToken successor = fixture.lifecycle.refresh(initialToken)
                .require(RefreshToken.KIND)
                .getToken();
        RefreshTokenSession session = fixture.store.getSession(fixture.store.getSessionId(initialDigest));
        fixture.clock.setInstant(ISSUED_AT.plus(MAXIMUM_LIFETIME));

        assertEquals(ISSUED_AT.plus(MAXIMUM_LIFETIME), session.getLifetime().getIdleExpiresAt());
        assertEquals(ISSUED_AT.plus(MAXIMUM_LIFETIME), session.getLifetime().getAbsoluteExpiresAt());
        assertThrows(AuthenticationException.class, () -> fixture.lifecycle.refresh(successor));
        assertEquals(2, fixture.store.digestCount());
    }

    @Test
    void revokesTheSessionWhenAConsumedTokenIsReplayed() {
        Fixture fixture = new Fixture();
        RefreshToken initialToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        TokenDigest<RefreshToken> initialDigest = fixture.hasher.hash(initialToken);
        fixture.clock.setInstant(ISSUED_AT.plusSeconds(1));
        RefreshToken successor = fixture.lifecycle.refresh(initialToken)
                .require(RefreshToken.KIND)
                .getToken();
        RefreshTokenSessionId sessionId = fixture.store.getSessionId(initialDigest);

        AuthenticationException replay = assertThrows(
                AuthenticationException.class,
                () -> fixture.lifecycle.refresh(initialToken)
        );

        assertEquals(AuthErrorCode.REFRESH_TOKEN_REJECTED, replay.getErrorCode());
        assertTrue(fixture.store.isRevoked(sessionId));
        assertThrows(AuthenticationException.class, () -> fixture.lifecycle.refresh(successor));
    }

    @Test
    void permitsAtMostOneConcurrentRotationAndRevokesTheWinnerSession() throws Exception {
        Fixture fixture = new Fixture();
        RefreshToken initialToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        TokenDigest<RefreshToken> initialDigest = fixture.hasher.hash(initialToken);
        fixture.clock.setInstant(ISSUED_AT.plusSeconds(1));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> refreshAfterSignal(fixture, initialToken, ready, start);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(attempt);
            Future<Boolean> second = executor.submit(attempt);
            ready.await();
            start.countDown();
            int successCount = Boolean.compare(first.get(), false) + Boolean.compare(second.get(), false);

            assertEquals(1, successCount);
            assertTrue(fixture.store.isRevoked(fixture.store.getSessionId(initialDigest)));
            assertEquals(2, fixture.store.digestCount());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsUnknownTokensWithoutCreatingState() {
        Fixture fixture = new Fixture();

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> fixture.lifecycle.refresh(new RefreshToken("unknown-refresh-token"))
        );

        assertEquals(AuthErrorCode.REFRESH_TOKEN_REJECTED, exception.getErrorCode());
        assertEquals(0, fixture.store.sessionCount());
        assertEquals(0, fixture.store.digestCount());
    }

    @Test
    void publishesTokenLifecycleResultsWithoutTokenValues() {
        List<TokenLifecycleEvent> events = new ArrayList<>();
        Fixture fixture = new Fixture(event -> events.add((TokenLifecycleEvent) event));

        TokenIssuance initialIssuance = fixture.lifecycle.issue(initialAuthentication());
        RefreshToken initialToken = initialIssuance.require(RefreshToken.KIND).getToken();
        fixture.clock.setInstant(ISSUED_AT.plusSeconds(1L));
        fixture.lifecycle.refresh(initialToken);
        assertThrows(AuthenticationException.class, () -> fixture.lifecycle.refresh(new RefreshToken("unknown")));

        assertEquals(3, events.size());
        assertEquals(TokenLifecycleEvent.Result.SUCCESS, events.get(0).getResult());
        assertEquals(TokenLifecycleEvent.Result.SUCCESS, events.get(1).getResult());
        assertEquals(TokenLifecycleEvent.Result.REJECTED, events.get(2).getResult());
        assertTrue(events.get(0).getTokenKinds().contains(AccessToken.KIND.getName()));
        assertTrue(events.get(0).getTokenKinds().contains(RefreshToken.KIND.getName()));
        assertFalse(events.get(0).getTokenKinds().contains(initialToken.getValue()));
    }

    @Test
    void publishesRefreshSessionRevocationWithoutSessionIdentifiers() {
        List<TokenLifecycleEvent> events = new ArrayList<>();
        Fixture fixture = new Fixture();
        RefreshToken initialToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        RefreshTokenSessionId sessionId = fixture.store.getSessionId(fixture.hasher.hash(initialToken));
        EventPublishingRefreshTokenSessionStore<RefreshTokenSession> store =
                new EventPublishingRefreshTokenSessionStore<>(
                        fixture.store,
                        event -> events.add((TokenLifecycleEvent) event),
                        Clock.fixed(ISSUED_AT, ZoneOffset.UTC)
                );

        store.revoke(sessionId, ISSUED_AT.plusSeconds(1L));

        assertEquals(1, events.size());
        assertEquals(TokenLifecycleEvent.Operation.REVOKE, events.get(0).getOperation());
        assertEquals(List.of(RefreshToken.KIND.getName()), List.copyOf(events.get(0).getTokenKinds()));
        assertTrue(fixture.store.isRevoked(sessionId));
    }

    @Test
    void revokesTheRotatedSessionWhenAuthenticationResolutionFails() {
        Fixture fixture = new Fixture();
        RefreshToken initialToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        TokenDigest<RefreshToken> initialDigest = fixture.hasher.hash(initialToken);
        RefreshTokenSessionId sessionId = fixture.store.getSessionId(initialDigest);
        fixture.authenticationResolver.failWith(new IllegalStateException("Subject resolution failed"));
        fixture.clock.setInstant(ISSUED_AT.plusSeconds(1));

        assertThrows(IllegalStateException.class, () -> fixture.lifecycle.refresh(initialToken));

        assertTrue(fixture.store.isRevoked(sessionId));
        assertTrue(fixture.store.isConsumed(initialDigest));
    }

    @Test
    void revokesTheRotatedSessionWhenAccessTokenIssueFails() {
        Fixture fixture = new Fixture();
        RefreshToken initialToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        TokenDigest<RefreshToken> initialDigest = fixture.hasher.hash(initialToken);
        RefreshTokenSessionId sessionId = fixture.store.getSessionId(initialDigest);
        fixture.accessTokenIssuer.failWith(new IllegalStateException("Access Token issuance failed"));
        fixture.clock.setInstant(ISSUED_AT.plusSeconds(1));

        assertThrows(IllegalStateException.class, () -> fixture.lifecycle.refresh(initialToken));

        assertTrue(fixture.store.isRevoked(sessionId));
        assertTrue(fixture.store.isConsumed(initialDigest));
    }

    @Test
    void revokesTheRotatedSessionWhenTheResolvedSubjectChanges() {
        Fixture fixture = new Fixture();
        RefreshToken initialToken = fixture.lifecycle.issue(initialAuthentication())
                .require(RefreshToken.KIND)
                .getToken();
        TokenDigest<RefreshToken> initialDigest = fixture.hasher.hash(initialToken);
        RefreshTokenSessionId sessionId = fixture.store.getSessionId(initialDigest);
        fixture.authenticationResolver.setAuthentication(new Authentication(
                new UserSubject(99L),
                grants(READ_SCOPE)
        ));
        fixture.clock.setInstant(ISSUED_AT.plusSeconds(1));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> fixture.lifecycle.refresh(initialToken)
        );

        assertEquals(AuthErrorCode.REFRESH_TOKEN_REJECTED, exception.getErrorCode());
        assertTrue(fixture.store.isRevoked(sessionId));
    }

    private static boolean refreshAfterSignal(
            Fixture fixture,
            RefreshToken refreshToken,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            fixture.lifecycle.refresh(refreshToken);
            return true;
        } catch (AuthenticationException exception) {
            return false;
        }
    }

    private static Authentication initialAuthentication() {
        return new Authentication(new UserSubject(42L), grants(READ_SCOPE, WRITE_SCOPE));
    }

    private static AuthorizationGrantSet grants(AuthorizationScope... scopes) {
        return AuthorizationGrantSet.of(List.of(scopes));
    }

    private static final class Fixture {
        private final MutableClock clock = new MutableClock(ISSUED_AT, ZoneOffset.UTC);
        private final InMemoryRefreshTokenSessionStore store = new InMemoryRefreshTokenSessionStore();
        private final Sha256TokenHasher<RefreshToken> hasher = new Sha256TokenHasher<>(RefreshToken.KIND);
        private final TestAccessTokenIssuer accessTokenIssuer = new TestAccessTokenIssuer(clock);
        private final TestAuthenticationResolver authenticationResolver = new TestAuthenticationResolver(
                initialAuthentication()
        );
        private final StoredRefreshTokenLifecycle<RefreshTokenSession> lifecycle;

        private Fixture() {
            this(IDLE_TIMEOUT, MAXIMUM_LIFETIME, event -> {
            });
        }

        private Fixture(Duration idleTimeout, Duration maximumLifetime) {
            this(idleTimeout, maximumLifetime, event -> {
            });
        }

        private Fixture(EventPublisher eventPublisher) {
            this(IDLE_TIMEOUT, MAXIMUM_LIFETIME, eventPublisher);
        }

        private Fixture(Duration idleTimeout, Duration maximumLifetime, EventPublisher eventPublisher) {
            lifecycle = new StoredRefreshTokenLifecycle<>(
                    store,
                    accessTokenIssuer,
                    hasher,
                    (id, authentication, lifetime) -> RefreshTokenSession.of(
                            id,
                            authentication.subject(),
                            authentication.grants(),
                            lifetime
                    ),
                    authenticationResolver,
                    new SecureRandom(),
                    clock,
                    idleTimeout,
                    maximumLifetime,
                    eventPublisher
            );
        }
    }

    private static final class TestAccessTokenIssuer implements TokenIssuer {
        private final Clock clock;
        private int issueCount;
        private Authentication lastAuthentication;
        private RuntimeException failure;

        private TestAccessTokenIssuer(Clock clock) {
            this.clock = clock;
        }

        @Override
        public synchronized TokenIssuance issue(Authentication authentication) {
            Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
            if (failure != null) {
                throw failure;
            }
            issueCount++;
            lastAuthentication = nonNullAuthentication;
            Instant issuedAt = clock.instant();
            return TokenIssuance.of(List.of(IssuedToken.of(
                    new AccessToken("access-" + issueCount),
                    issuedAt,
                    issuedAt.plus(Duration.ofMinutes(5))
            )));
        }

        private synchronized Authentication getLastAuthentication() {
            return lastAuthentication;
        }

        private synchronized void failWith(RuntimeException failure) {
            this.failure = Objects.requireNonNull(failure, "failure");
        }
    }

    private static final class TestAuthenticationResolver
            implements RefreshTokenAuthenticationResolver<RefreshTokenSession> {
        private Authentication authentication;
        private RuntimeException failure;

        private TestAuthenticationResolver(Authentication authentication) {
            this.authentication = authentication;
        }

        @Override
        public synchronized Authentication resolve(RefreshTokenSession session) {
            Objects.requireNonNull(session, "session");
            if (failure != null) {
                throw failure;
            }
            return authentication;
        }

        private synchronized void setAuthentication(Authentication authentication) {
            this.authentication = Objects.requireNonNull(authentication, "authentication");
        }

        private synchronized void failWith(RuntimeException failure) {
            this.failure = Objects.requireNonNull(failure, "failure");
        }
    }

    private static final class MutableClock extends Clock {
        private volatile Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = Objects.requireNonNull(instant, "instant");
            this.zone = Objects.requireNonNull(zone, "zone");
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void setInstant(Instant instant) {
            this.instant = Objects.requireNonNull(instant, "instant");
        }
    }
}
