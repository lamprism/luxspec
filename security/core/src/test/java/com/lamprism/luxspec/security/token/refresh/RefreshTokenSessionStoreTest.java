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

import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.SessionLifetime;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenRotation;
import com.lamprism.luxspec.security.token.TokenRotationRejection;
import com.lamprism.luxspec.security.token.TokenRotationResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenSessionStoreTest {
    private static final Instant CREATED_AT = Instant.parse("2026-07-15T00:00:00Z");

    @Test
    void atomicallyRotatesAndRenewsTheSession() {
        InMemoryRefreshTokenSessionStore store = new InMemoryRefreshTokenSessionStore();
        RefreshTokenSession session = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenDigest<RefreshToken> presented = digest(1);
        TokenDigest<RefreshToken> successor = digest(2);
        store.create(session, presented);

        TokenRotationResult<RefreshTokenSession> result = store.rotate(new TokenRotation<>(
                presented,
                successor,
                CREATED_AT.plus(Duration.ofMinutes(5))
        ));

        TokenRotationResult.Succeeded<RefreshTokenSession> succeeded = requireSucceeded(result);
        assertEquals(
                CREATED_AT.plus(Duration.ofMinutes(5)),
                succeeded.getState().getLifetime().getLastActivityAt()
        );
        assertEquals(CREATED_AT.plus(Duration.ofMinutes(15)), succeeded.getState().getLifetime().getIdleExpiresAt());
        assertTrue(store.isConsumed(presented));
        assertFalse(store.isConsumed(successor));
        assertEquals(2, store.getDigestCount());
    }

    @Test
    void revokesTheSessionWhenAConsumedDigestIsReplayed() {
        InMemoryRefreshTokenSessionStore store = new InMemoryRefreshTokenSessionStore();
        RefreshTokenSession session = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenRotation<RefreshToken> rotation = new TokenRotation<>(
                digest(1),
                digest(2),
                CREATED_AT.plusSeconds(1)
        );
        store.create(session, rotation.getPresentedDigest());
        requireSucceeded(store.rotate(rotation));

        TokenRotationResult.Rejected<RefreshTokenSession> replay = requireRejected(store.rotate(rotation));

        assertEquals(TokenRotationRejection.REPLAY_DETECTED, replay.getReason());
        assertTrue(store.isRevoked(session.getId()));
        assertEquals(
                TokenRotationRejection.REVOKED,
                requireRejected(store.rotate(new TokenRotation<>(
                        rotation.getSuccessorDigest(),
                        digest(3),
                        CREATED_AT.plusSeconds(2)
                ))).getReason()
        );
    }

    @Test
    void rejectsExpiredRotationWithoutInstallingASuccessor() {
        InMemoryRefreshTokenSessionStore store = new InMemoryRefreshTokenSessionStore();
        RefreshTokenSession session = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenDigest<RefreshToken> presented = digest(1);
        store.create(session, presented);

        TokenRotationResult.Rejected<RefreshTokenSession> rejected = requireRejected(store.rotate(
                new TokenRotation<>(presented, digest(2), CREATED_AT.plus(Duration.ofMinutes(10)))
        ));

        assertEquals(TokenRotationRejection.EXPIRED, rejected.getReason());
        assertEquals(1, store.getDigestCount());
        assertFalse(store.isConsumed(presented));
    }

    @Test
    void rejectsDuplicateCreationWithoutPartialMutation() {
        InMemoryRefreshTokenSessionStore store = new InMemoryRefreshTokenSessionStore();
        RefreshTokenSession first = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        RefreshTokenSession duplicateId = session("session-1", Duration.ofMinutes(10), Duration.ofHours(1));
        RefreshTokenSession second = session("session-2", Duration.ofMinutes(10), Duration.ofHours(1));
        TokenDigest<RefreshToken> digest = digest(1);
        store.create(first, digest);

        assertThrows(IllegalStateException.class, () -> store.create(duplicateId, digest(2)));
        assertThrows(IllegalStateException.class, () -> store.create(second, digest));
        assertEquals(1, store.getSessionCount());
        assertEquals(1, store.getDigestCount());
    }

    @Test
    void revokesIdempotentlyBySessionIdOrDigest() {
        InMemoryRefreshTokenSessionStore store = new InMemoryRefreshTokenSessionStore();
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

    private static TokenRotationResult.Succeeded<RefreshTokenSession> requireSucceeded(
            TokenRotationResult<RefreshTokenSession> result
    ) {
        if (result instanceof TokenRotationResult.Succeeded<RefreshTokenSession> succeeded) {
            return succeeded;
        }
        throw new AssertionError("Expected successful rotation");
    }

    private static TokenRotationResult.Rejected<RefreshTokenSession> requireRejected(
            TokenRotationResult<RefreshTokenSession> result
    ) {
        if (result instanceof TokenRotationResult.Rejected<RefreshTokenSession> rejected) {
            return rejected;
        }
        throw new AssertionError("Expected rejected rotation");
    }
}
