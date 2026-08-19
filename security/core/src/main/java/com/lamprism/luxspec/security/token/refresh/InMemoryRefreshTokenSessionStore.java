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

import com.lamprism.luxspec.security.token.SessionLifetime;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenRotation;
import com.lamprism.luxspec.security.token.TokenRotationRejection;
import com.lamprism.luxspec.security.token.TokenRotationResult;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides a concurrent, one-JVM refresh-token session store with atomic rotation.
 *
 * <p>Each session has its own mutation monitor, so unrelated sessions can rotate concurrently.
 * Digests remain available until their session reaches its absolute expiration; {@link #cleanup()}
 * removes expired sessions and all of their digest history. Replaying a consumed digest revokes
 * the complete session, matching the refresh-token reuse detection contract.</p>
 *
 * @author RollW
 */
public class InMemoryRefreshTokenSessionStore implements RefreshTokenSessionStore<RefreshTokenSession> {
    private final Clock clock;
    private final Object creationLock = new Object();
    private final ConcurrentMap<RefreshTokenSessionId, StoredSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<TokenDigest<RefreshToken>, StoredDigest> digests = new ConcurrentHashMap<>();

    /**
     * Creates a store using the UTC system clock.
     */
    public InMemoryRefreshTokenSessionStore() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a store using an explicit expiration clock.
     *
     * @param clock the clock used for cleanup and expiration checks
     */
    public InMemoryRefreshTokenSessionStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void create(
            RefreshTokenSession session,
            TokenDigest<RefreshToken> initialDigest
    ) {
        RefreshTokenSession nonNullSession = Objects.requireNonNull(session, "session");
        TokenDigest<RefreshToken> nonNullDigest = requireDigest(initialDigest);
        StoredSession storedSession = new StoredSession(nonNullSession);
        StoredDigest storedDigest = new StoredDigest(nonNullSession.getId());
        synchronized (creationLock) {
            synchronized (storedSession) {
                if (sessions.putIfAbsent(nonNullSession.getId(), storedSession) != null) {
                    throw new IllegalStateException("Refresh Token Session ID already exists");
                }
                if (digests.putIfAbsent(nonNullDigest, storedDigest) != null) {
                    sessions.remove(nonNullSession.getId(), storedSession);
                    throw new IllegalStateException("Refresh Token Digest already exists");
                }
                storedSession.digests.put(nonNullDigest, storedDigest);
            }
        }
    }

    @Override
    public TokenRotationResult<RefreshTokenSession> rotate(TokenRotation<RefreshToken> rotation) {
        TokenRotation<RefreshToken> nonNullRotation = Objects.requireNonNull(rotation, "rotation");
        TokenDigest<RefreshToken> presentedDigest = requireDigest(nonNullRotation.getPresentedDigest());
        TokenDigest<RefreshToken> successorDigest = requireDigest(nonNullRotation.getSuccessorDigest());
        StoredDigest presented = digests.get(presentedDigest);
        if (presented == null) {
            return TokenRotationResult.rejected(TokenRotationRejection.UNKNOWN);
        }
        StoredSession storedSession = sessions.get(presented.sessionId);
        if (storedSession == null) {
            return TokenRotationResult.rejected(TokenRotationRejection.UNKNOWN);
        }
        synchronized (storedSession) {
            if (sessions.get(presented.sessionId) != storedSession
                    || digests.get(presentedDigest) != presented) {
                return TokenRotationResult.rejected(TokenRotationRejection.UNKNOWN);
            }
            if (storedSession.revoked) {
                return TokenRotationResult.rejected(TokenRotationRejection.REVOKED);
            }
            if (presented.consumed) {
                storedSession.revoked = true;
                return TokenRotationResult.rejected(TokenRotationRejection.REPLAY_DETECTED);
            }
            Instant rotatedAt = Objects.requireNonNull(nonNullRotation.getRotatedAt(), "rotatedAt");
            if (storedSession.session.getLifetime().isExpiredAt(rotatedAt)) {
                return TokenRotationResult.rejected(TokenRotationRejection.EXPIRED);
            }
            SessionLifetime renewedLifetime = storedSession.session.getLifetime().renew(rotatedAt);
            RefreshTokenSession renewedSession = RefreshTokenSession.of(
                    storedSession.session.getId(),
                    storedSession.session.getSubject(),
                    storedSession.session.getAuthorizationCeiling(),
                    renewedLifetime
            );
            StoredDigest successor = new StoredDigest(storedSession.session.getId());
            if (digests.putIfAbsent(successorDigest, successor) != null) {
                throw new IllegalStateException("Refresh Token successor Digest already exists");
            }
            presented.consumed = true;
            storedSession.session = renewedSession;
            storedSession.digests.put(successorDigest, successor);
            return TokenRotationResult.succeeded(renewedSession);
        }
    }

    @Override
    public void revoke(RefreshTokenSessionId sessionId, Instant revokedAt) {
        RefreshTokenSessionId nonNullSessionId = Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(revokedAt, "revokedAt");
        StoredSession storedSession = sessions.get(nonNullSessionId);
        if (storedSession != null) {
            synchronized (storedSession) {
                storedSession.revoked = true;
            }
        }
    }

    @Override
    public void revoke(TokenDigest<RefreshToken> digest, Instant revokedAt) {
        TokenDigest<RefreshToken> nonNullDigest = requireDigest(digest);
        Objects.requireNonNull(revokedAt, "revokedAt");
        StoredDigest storedDigest = digests.get(nonNullDigest);
        if (storedDigest == null) {
            return;
        }
        StoredSession storedSession = sessions.get(storedDigest.sessionId);
        if (storedSession == null) {
            return;
        }
        synchronized (storedSession) {
            if (sessions.get(storedDigest.sessionId) == storedSession
                    && digests.get(nonNullDigest) == storedDigest) {
                storedSession.revoked = true;
            }
        }
    }

    /**
     * Removes sessions whose absolute lifetime has ended and all digest history owned by them.
     *
     * @return the number of removed sessions
     */
    public int cleanup() {
        Instant now = Objects.requireNonNull(clock.instant(), "clock instant");
        int removed = 0;
        for (Map.Entry<RefreshTokenSessionId, StoredSession> entry : sessions.entrySet()) {
            StoredSession storedSession = entry.getValue();
            synchronized (storedSession) {
                if (!storedSession.session.getLifetime().getAbsoluteExpiresAt().isAfter(now)
                        && sessions.remove(entry.getKey(), storedSession)) {
                    for (Map.Entry<TokenDigest<RefreshToken>, StoredDigest> digest
                            : storedSession.digests.entrySet()) {
                        digests.remove(digest.getKey(), digest.getValue());
                    }
                    storedSession.digests.clear();
                    removed++;
                }
            }
        }
        return removed;
    }

    /**
     * Returns the current number of sessions.
     *
     * @return the session count
     */
    public int sessionCount() {
        return sessions.size();
    }

    /**
     * Returns the current number of retained digests.
     *
     * @return the digest count
     */
    public int digestCount() {
        return digests.size();
    }

    /**
     * Returns whether a session is currently revoked.
     *
     * @param sessionId the session identifier
     * @return whether the session is revoked
     */
    public boolean isRevoked(RefreshTokenSessionId sessionId) {
        StoredSession storedSession = sessions.get(Objects.requireNonNull(sessionId, "sessionId"));
        if (storedSession == null) {
            return false;
        }
        synchronized (storedSession) {
            return sessions.get(sessionId) == storedSession && storedSession.revoked;
        }
    }

    RefreshTokenSession getSession(RefreshTokenSessionId sessionId) {
        RefreshTokenSessionId nonNullSessionId = Objects.requireNonNull(sessionId, "sessionId");
        StoredSession storedSession = sessions.get(nonNullSessionId);
        if (storedSession == null) {
            throw new IllegalArgumentException("Unknown Refresh Token Session");
        }
        synchronized (storedSession) {
            if (sessions.get(nonNullSessionId) != storedSession) {
                throw new IllegalArgumentException("Unknown Refresh Token Session");
            }
            return storedSession.session;
        }
    }

    RefreshTokenSessionId getSessionId(TokenDigest<RefreshToken> digest) {
        StoredDigest storedDigest = digests.get(requireDigest(digest));
        if (storedDigest == null) {
            throw new IllegalArgumentException("Unknown Refresh Token Digest");
        }
        return storedDigest.sessionId;
    }

    boolean isConsumed(TokenDigest<RefreshToken> digest) {
        TokenDigest<RefreshToken> nonNullDigest = requireDigest(digest);
        StoredDigest storedDigest = digests.get(nonNullDigest);
        if (storedDigest == null) {
            return false;
        }
        StoredSession storedSession = sessions.get(storedDigest.sessionId);
        if (storedSession == null) {
            return false;
        }
        synchronized (storedSession) {
            return sessions.get(storedDigest.sessionId) == storedSession
                    && digests.get(nonNullDigest) == storedDigest
                    && storedDigest.consumed;
        }
    }

    private static TokenDigest<RefreshToken> requireDigest(TokenDigest<RefreshToken> digest) {
        TokenDigest<RefreshToken> nonNullDigest = Objects.requireNonNull(digest, "digest");
        if (!RefreshToken.KIND.equals(nonNullDigest.getTokenKind())) {
            throw new IllegalArgumentException("Digest must belong to the Refresh Token kind");
        }
        return nonNullDigest;
    }

    private static final class StoredSession {
        private final Map<TokenDigest<RefreshToken>, StoredDigest> digests = new LinkedHashMap<>();
        private RefreshTokenSession session;
        private boolean revoked;

        private StoredSession(RefreshTokenSession session) {
            this.session = session;
        }
    }

    private static final class StoredDigest {
        private final RefreshTokenSessionId sessionId;
        private boolean consumed;

        private StoredDigest(RefreshTokenSessionId sessionId) {
            this.sessionId = sessionId;
        }
    }
}
