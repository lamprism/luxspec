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

import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.SessionLifetime;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenRotationException;
import com.lamprism.luxspec.security.token.TokenRotationRejection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class RefreshTokenSessionStoreTest {
    private static final Instant CREATED_AT = Instant.parse("2026-07-15T00:00:00Z");

    @Test
    void atomicallyRotatesAndRenewsTheSession() {
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store = InMemoryRefreshTokenSessionStore.standard();
        RefreshTokenSession session = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenDigest<RefreshToken> presented = digest(1);
        TokenDigest<RefreshToken> successor = digest(2);
        store.create(session, presented);

        RefreshTokenSession renewed = store.rotate(
                presented,
                successor,
                CREATED_AT.plus(Duration.ofMinutes(5))
        );

        assertEquals(
                CREATED_AT.plus(Duration.ofMinutes(5)),
                renewed.getLifetime().getLastActivityAt()
        );
        assertEquals(CREATED_AT.plus(Duration.ofMinutes(15)), renewed.getLifetime().getIdleExpiresAt());
        assertTrue(store.isConsumed(presented));
        assertFalse(store.isConsumed(successor));
        assertEquals(2, store.digestCount());
    }

    @Test
    void revokesTheSessionWhenAConsumedDigestIsReplayed() {
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store = InMemoryRefreshTokenSessionStore.standard();
        RefreshTokenSession session = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        store.create(session, digest(1));
        store.rotate(digest(1), digest(2), CREATED_AT.plusSeconds(1));

        TokenRotationException replay = assertThrows(TokenRotationException.class,
                () -> store.rotate(digest(1), digest(2), CREATED_AT.plusSeconds(1)));

        assertEquals(TokenRotationRejection.REPLAY_DETECTED, replay.getReason());
        assertTrue(store.isRevoked(session.getId()));
        assertEquals(
                TokenRotationRejection.REVOKED,
                assertThrows(TokenRotationException.class,
                        () -> store.rotate(digest(2), digest(3), CREATED_AT.plusSeconds(2))).getReason()
        );
    }

    @Test
    void preservesApplicationMetadataAndSupportsRejectOnlyReplayPolicy() {
        RefreshTokenSessionRenewal<VersionedSession> renewal = (original, lifetime) -> new VersionedSession(
                RefreshTokenSession.of(
                        original.getId(), original.getSubject(), original.getAuthorizationCeiling(), lifetime
                ),
                original.securityVersion()
        );
        AtomicReference<VersionedSession> replayed = new AtomicReference<>();
        InMemoryRefreshTokenSessionStore<VersionedSession> store = new InMemoryRefreshTokenSessionStore<>(
                Clock.fixed(CREATED_AT, ZoneOffset.UTC), renewal, RefreshTokenReplayPolicy.rejectOnly(),
                (replayedSession, detectedAt) -> replayed.set(replayedSession)
        );
        VersionedSession session = new VersionedSession(
                session("session-1", Duration.ofMinutes(10), Duration.ofHours(1)), "version-7"
        );
        store.create(session, digest(1));
        VersionedSession renewed = store.rotate(digest(1), digest(2), CREATED_AT.plusSeconds(1));
        assertEquals("version-7", renewed.securityVersion());

        TokenRotationException rejected = assertThrows(TokenRotationException.class,
                () -> store.rotate(digest(1), digest(2), CREATED_AT.plusSeconds(1)));
        assertEquals(TokenRotationRejection.REPLAY_DETECTED, rejected.getReason());
        assertEquals("version-7", replayed.get().securityVersion());
        assertFalse(store.isRevoked(session.getId()));
        assertEquals("version-7", store.rotate(digest(2), digest(3), CREATED_AT.plusSeconds(2)).securityVersion());
    }

    @Test
    void rejectsAnInvalidRenewalWithoutConsumingTheToken() {
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store = new InMemoryRefreshTokenSessionStore<>(
                Clock.fixed(CREATED_AT, ZoneOffset.UTC),
                (original, lifetime) -> session("another-session", Duration.ofMinutes(10), Duration.ofHours(1)),
                RefreshTokenReplayPolicy.revokeSession()
        );
        RefreshTokenSession session = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        store.create(session, digest(1));

        assertThrows(IllegalStateException.class,
                () -> store.rotate(digest(1), digest(2), CREATED_AT.plusSeconds(1)));
        assertFalse(store.isConsumed(digest(1)));
        assertEquals(1, store.digestCount());
    }

    @Test
    void rejectsExpiredRotationWithoutInstallingASuccessor() {
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store = InMemoryRefreshTokenSessionStore.standard();
        RefreshTokenSession session = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenDigest<RefreshToken> presented = digest(1);
        store.create(session, presented);

        TokenRotationException rejected = assertThrows(TokenRotationException.class,
                () -> store.rotate(presented, digest(2), CREATED_AT.plus(Duration.ofMinutes(10))));

        assertEquals(TokenRotationRejection.EXPIRED, rejected.getReason());
        assertEquals(1, store.digestCount());
        assertFalse(store.isConsumed(presented));
    }

    @Test
    void rejectsAnIdenticalSuccessorBeforeChangingState() {
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store = InMemoryRefreshTokenSessionStore.standard();
        store.create(session("session-1", Duration.ofMinutes(10), Duration.ofHours(1)), digest(1));

        assertThrows(IllegalArgumentException.class,
                () -> store.rotate(digest(1), digest(1), CREATED_AT.plusSeconds(1)));
        assertFalse(store.isConsumed(digest(1)));
    }

    @Test
    void rejectsDuplicateCreationWithoutPartialMutation() {
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store = InMemoryRefreshTokenSessionStore.standard();
        RefreshTokenSession first = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        RefreshTokenSession duplicateId = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        RefreshTokenSession second = session("session-2", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenDigest<RefreshToken> digest = digest(1);
        store.create(first, digest);

        assertThrows(IllegalStateException.class, () -> store.create(duplicateId, digest(2)));
        assertThrows(IllegalStateException.class, () -> store.create(second, digest));
        assertEquals(1, store.sessionCount());
        assertEquals(1, store.digestCount());
    }

    @Test
    void revokesIdempotentlyBySessionIdOrDigest() {
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store = InMemoryRefreshTokenSessionStore.standard();
        RefreshTokenSession first = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        RefreshTokenSession second = session("session-2", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenDigest<RefreshToken> firstDigest = digest(1);
        TokenDigest<RefreshToken> secondDigest = digest(2);
        store.create(first, firstDigest);
        store.create(second, secondDigest);

        store.revoke(first.getId(), CREATED_AT);
        store.revoke(first.getId(), CREATED_AT.plusSeconds(1));
        store.revoke(secondDigest, CREATED_AT);
        store.revoke(secondDigest, CREATED_AT.plusSeconds(1));
        store.revoke(digest(9), CREATED_AT);

        assertTrue(store.isRevoked(first.getId()));
        assertTrue(store.isRevoked(second.getId()));
    }

    @Test
    void neverAssociatesAnExpiredDigestWithARecreatedSessionDuringCleanup() throws Exception {
        Instant expiration = CREATED_AT.plus(Duration.ofHours(1));
        InMemoryRefreshTokenSessionStore<RefreshTokenSession> store =
                InMemoryRefreshTokenSessionStore.standard(Clock.fixed(expiration, ZoneOffset.UTC));
        BlockingDigestMap digestMap = new BlockingDigestMap();
        Field digests = InMemoryRefreshTokenSessionStore.class.getDeclaredField("digests");
        digests.setAccessible(true);
        digests.set(store, digestMap);
        RefreshTokenSession expired = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        RefreshTokenSession replacement = RefreshTokenSession.of(
                expired.getId(), new UserSubject(99L), AuthorizationGrantSet.of(List.of()),
                SessionLifetime.start(expiration, Duration.ofMinutes(10), Duration.ofHours(1))
        );
        store.create(expired, digest(1));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> cleanup = executor.submit(store::cleanup);
        boolean created = false;
        try {
            assertTrue(digestMap.removalStarted.await(5, TimeUnit.SECONDS));
            try {
                store.create(replacement, digest(2));
                created = true;
            } catch (IllegalStateException expected) {
                /*
                 * The old session can remain authoritative until its digest history is removed.
                 */
            }
            if (created) {
                TokenRotationException rejection = assertThrows(TokenRotationException.class,
                        () -> store.rotate(digest(1), digest(3), expiration.plusSeconds(1)));
                assertEquals(TokenRotationRejection.UNKNOWN, rejection.getReason());
            }
        } finally {
            digestMap.allowRemoval.countDown();
            cleanup.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        if (!created) {
            store.create(replacement, digest(2));
        }
        assertEquals(TokenRotationRejection.UNKNOWN, assertThrows(TokenRotationException.class,
                () -> store.rotate(digest(1), digest(3), expiration.plusSeconds(1))).getReason());
    }

    private static final class BlockingDigestMap extends ConcurrentHashMap<Object, Object> {
        private final CountDownLatch removalStarted = new CountDownLatch(1);
        private final CountDownLatch allowRemoval = new CountDownLatch(1);

        @Override
        public boolean remove(Object key, Object value) {
            removalStarted.countDown();
            try {
                if (!allowRemoval.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for digest cleanup");
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Digest cleanup was interrupted", failure);
            }
            return super.remove(key, value);
        }
    }

    private static RefreshTokenSession session(
            String id,
            Duration idleTimeout,
            Duration maximumLifetime
    ) {
        return RefreshTokenSession.of(
                new RefreshTokenSessionId(id),
                new UserSubject(42L),
                AuthorizationGrantSet.of(List.of()),
                SessionLifetime.start(CREATED_AT, idleTimeout, maximumLifetime)
        );
    }

    private static TokenDigest<RefreshToken> digest(int value) {
        return new TokenDigest<>(RefreshToken.KIND, new byte[]{(byte) value});
    }

    private static final class VersionedSession implements RefreshTokenSession {
        private final RefreshTokenSession session;
        private final String securityVersion;

        private VersionedSession(RefreshTokenSession session, String securityVersion) {
            this.session = session;
            this.securityVersion = securityVersion;
        }

        private String securityVersion() {
            return securityVersion;
        }

        @Override
        public RefreshTokenSessionId getId() {
            return session.getId();
        }

        @Override
        public Subject getSubject() {
            return session.getSubject();
        }

        @Override
        public SessionLifetime getLifetime() {
            return session.getLifetime();
        }

        @Override
        public AuthorizationGrantSet getAuthorizationCeiling() {
            return session.getAuthorizationCeiling();
        }
    }
}
