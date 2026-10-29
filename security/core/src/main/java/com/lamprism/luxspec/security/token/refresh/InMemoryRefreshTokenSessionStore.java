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

import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.token.SessionLifetime;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenRotationException;
import com.lamprism.luxspec.security.token.TokenRotationRejection;

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
 * removes expired sessions and all of their digest history. Replay handling and session renewal
 * are supplied as strategies.</p>
 *
 * @param <S> the application session type
 *
 * @author RollW
 */
public class InMemoryRefreshTokenSessionStore<S extends RefreshTokenSession> implements RefreshTokenSessionStore<S> {
    private final Clock clock;
    private final RefreshTokenSessionRenewal<S> renewal;
    private final RefreshTokenReplayPolicy<S> replayPolicy;
    private final RefreshTokenReplayHandler<S> replayHandler;
    private final Object creationLock = new Object();
    private final ConcurrentMap<RefreshTokenSessionId, StoredSession<S>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<TokenDigest<RefreshToken>, StoredDigest> digests = new ConcurrentHashMap<>();

    /**
     * Creates a standard store using the UTC system clock and session revocation on replay.
     *
     * @return the standard in-memory store
     */
    public static InMemoryRefreshTokenSessionStore<RefreshTokenSession> standard() {
        return standard(Clock.systemUTC());
    }

    /**
     * Creates a standard store using an explicit expiration clock.
     *
     * @param clock the clock used for cleanup and expiration checks
     * @return the standard in-memory store
     */
    public static InMemoryRefreshTokenSessionStore<RefreshTokenSession> standard(Clock clock) {
        return new InMemoryRefreshTokenSessionStore<>(
                clock, RefreshTokenSessionRenewal.standard(), RefreshTokenReplayPolicy.revokeSession()
        );
    }

    /**
     * Creates a store for application session metadata and replay behavior.
     *
     * @param clock the clock used for cleanup and expiration checks
     * @param renewal the session renewal strategy
     * @param replayPolicy the atomic session replay policy
     */
    public InMemoryRefreshTokenSessionStore(
            Clock clock,
            RefreshTokenSessionRenewal<S> renewal,
            RefreshTokenReplayPolicy<S> replayPolicy
    ) {
        this(clock, renewal, replayPolicy, RefreshTokenReplayHandler.none());
    }

    /**
     * Creates a store with an application-selected response after a replay decision.
     *
     * @param clock         the clock used for cleanup and expiration checks
     * @param renewal       the session renewal strategy
     * @param replayPolicy  the atomic session replay policy
     * @param replayHandler the response invoked outside the session lock
     */
    public InMemoryRefreshTokenSessionStore(
            Clock clock,
            RefreshTokenSessionRenewal<S> renewal,
            RefreshTokenReplayPolicy<S> replayPolicy,
            RefreshTokenReplayHandler<S> replayHandler
    ) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.renewal = Objects.requireNonNull(renewal, "renewal");
        this.replayPolicy = Objects.requireNonNull(replayPolicy, "replayPolicy");
        this.replayHandler = Objects.requireNonNull(replayHandler, "replayHandler");
    }

    @Override
    public void create(
            S session,
            TokenDigest<RefreshToken> initialDigest
    ) {
        S nonNullSession = Objects.requireNonNull(session, "session");
        TokenDigest<RefreshToken> nonNullDigest = requireDigest(initialDigest);
        StoredSession<S> storedSession = new StoredSession<>(nonNullSession);
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
    public S rotate(TokenDigest<RefreshToken> presented, TokenDigest<RefreshToken> successor, Instant rotatedAt) {
        TokenDigest<RefreshToken> presentedDigest = requireDigest(presented);
        TokenDigest<RefreshToken> successorDigest = requireDigest(successor);
        Instant time = Objects.requireNonNull(rotatedAt, "rotatedAt");
        if (presentedDigest.equals(successorDigest)) {
            throw new IllegalArgumentException("Token rotation successor must differ from the presented digest");
        }
        StoredDigest presentedRecord = digests.get(presentedDigest);
        if (presentedRecord == null) {
            throw new TokenRotationException(TokenRotationRejection.UNKNOWN);
        }
        StoredSession<S> storedSession = sessions.get(presentedRecord.sessionId);
        if (storedSession == null) {
            throw new TokenRotationException(TokenRotationRejection.UNKNOWN);
        }
        S replayedSession;
        synchronized (storedSession) {
            if (sessions.get(presentedRecord.sessionId) != storedSession
                    || digests.get(presentedDigest) != presentedRecord) {
                throw new TokenRotationException(TokenRotationRejection.UNKNOWN);
            }
            if (storedSession.revoked) {
                throw new TokenRotationException(TokenRotationRejection.REVOKED);
            }
            if (presentedRecord.consumed) {
                if (replayPolicy.revokeSessionOnReplay(storedSession.session)) {
                    storedSession.revoked = true;
                }
                replayedSession = storedSession.session;
            } else {
                if (storedSession.session.getLifetime().isExpiredAt(time)) {
                    throw new TokenRotationException(TokenRotationRejection.EXPIRED);
                }
                SessionLifetime renewedLifetime = storedSession.session.getLifetime().renew(time);
                S renewedSession = Objects.requireNonNull(
                        renewal.renew(storedSession.session, renewedLifetime), "renewed session"
                );
                validateRenewal(storedSession.session, renewedSession, renewedLifetime);
                StoredDigest successorRecord = new StoredDigest(storedSession.session.getId());
                if (digests.putIfAbsent(successorDigest, successorRecord) != null) {
                    throw new IllegalStateException("Refresh Token successor Digest already exists");
                }
                presentedRecord.consumed = true;
                storedSession.session = renewedSession;
                storedSession.digests.put(successorDigest, successorRecord);
                return renewedSession;
            }
        }
        replayHandler.onReplay(replayedSession, time);
        throw new TokenRotationException(TokenRotationRejection.REPLAY_DETECTED);
    }

    @Override
    public void revoke(RefreshTokenSessionId sessionId, Instant revokedAt) {
        RefreshTokenSessionId nonNullSessionId = Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(revokedAt, "revokedAt");
        StoredSession<S> storedSession = sessions.get(nonNullSessionId);
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
        StoredSession<S> storedSession = sessions.get(storedDigest.sessionId);
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
        for (Map.Entry<RefreshTokenSessionId, StoredSession<S>> entry : sessions.entrySet()) {
            StoredSession<S> storedSession = entry.getValue();
            synchronized (storedSession) {
                if (!storedSession.session.getLifetime().getAbsoluteExpiresAt().isAfter(now)
                        && sessions.get(entry.getKey()) == storedSession) {
                    for (Map.Entry<TokenDigest<RefreshToken>, StoredDigest> digest
                            : storedSession.digests.entrySet()) {
                        digests.remove(digest.getKey(), digest.getValue());
                    }
                    storedSession.digests.clear();
                    if (sessions.remove(entry.getKey(), storedSession)) {
                        removed++;
                    }
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
        StoredSession<S> storedSession = sessions.get(Objects.requireNonNull(sessionId, "sessionId"));
        if (storedSession == null) {
            return false;
        }
        synchronized (storedSession) {
            return sessions.get(sessionId) == storedSession && storedSession.revoked;
        }
    }

    S getSession(RefreshTokenSessionId sessionId) {
        RefreshTokenSessionId nonNullSessionId = Objects.requireNonNull(sessionId, "sessionId");
        StoredSession<S> storedSession = sessions.get(nonNullSessionId);
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
        StoredSession<S> storedSession = sessions.get(storedDigest.sessionId);
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

    private void validateRenewal(S original, S renewed, SessionLifetime lifetime) {
        if (!original.getId().equals(renewed.getId())
                || !SimpleSubject.from(original.getSubject()).equals(SimpleSubject.from(renewed.getSubject()))
                || !original.getAuthorizationCeiling().equals(renewed.getAuthorizationCeiling())
                || !lifetime.equals(renewed.getLifetime())) {
            throw new IllegalStateException("Refresh Token Session renewal changed its identity or grants");
        }
    }

    private static final class StoredSession<S extends RefreshTokenSession> {
        private final Map<TokenDigest<RefreshToken>, StoredDigest> digests = new LinkedHashMap<>();
        private S session;
        private boolean revoked;

        private StoredSession(S session) {
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
