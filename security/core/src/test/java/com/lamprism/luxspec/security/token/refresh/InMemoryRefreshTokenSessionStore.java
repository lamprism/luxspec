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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class InMemoryRefreshTokenSessionStore
        implements RefreshTokenSessionStore<RefreshTokenSession> {
    private final Map<RefreshTokenSessionId, StoredSession> sessions = new HashMap<>();
    private final Map<TokenDigest<RefreshToken>, StoredDigest> digests = new HashMap<>();

    @Override
    public synchronized void create(
            RefreshTokenSession session,
            TokenDigest<RefreshToken> initialDigest
    ) {
        RefreshTokenSession nonNullSession = Objects.requireNonNull(session, "session");
        TokenDigest<RefreshToken> nonNullDigest = Objects.requireNonNull(initialDigest, "initialDigest");
        if (sessions.containsKey(nonNullSession.getId())) {
            throw new IllegalStateException("Refresh Token Session ID already exists");
        }
        if (digests.containsKey(nonNullDigest)) {
            throw new IllegalStateException("Refresh Token Digest already exists");
        }
        sessions.put(nonNullSession.getId(), new StoredSession(nonNullSession));
        digests.put(nonNullDigest, new StoredDigest(nonNullSession.getId()));
    }

    @Override
    public synchronized TokenRotationResult<RefreshTokenSession> rotate(
            TokenRotation<RefreshToken> rotation
    ) {
        TokenRotation<RefreshToken> nonNullRotation = Objects.requireNonNull(rotation, "rotation");
        StoredDigest presented = digests.get(nonNullRotation.getPresentedDigest());
        if (presented == null) {
            return TokenRotationResult.rejected(TokenRotationRejection.UNKNOWN);
        }
        StoredSession storedSession = requireStoredSession(presented.sessionId);
        if (storedSession.revoked) {
            return TokenRotationResult.rejected(TokenRotationRejection.REVOKED);
        }
        if (presented.consumed) {
            storedSession.revoked = true;
            return TokenRotationResult.rejected(TokenRotationRejection.REPLAY_DETECTED);
        }
        Instant rotatedAt = nonNullRotation.getRotatedAt();
        if (storedSession.session.getLifetime().isExpiredAt(rotatedAt)) {
            return TokenRotationResult.rejected(TokenRotationRejection.EXPIRED);
        }
        TokenDigest<RefreshToken> successorDigest = nonNullRotation.getSuccessorDigest();
        if (digests.containsKey(successorDigest)) {
            throw new IllegalStateException("Refresh Token successor Digest already exists");
        }
        SessionLifetime renewedLifetime = storedSession.session.getLifetime().renew(rotatedAt);
        RefreshTokenSession renewedSession = RefreshTokenSession.of(
                storedSession.session.getId(),
                storedSession.session.getSubject(),
                storedSession.session.getAuthorizationCeiling(),
                renewedLifetime
        );
        presented.consumed = true;
        storedSession.session = renewedSession;
        digests.put(successorDigest, new StoredDigest(presented.sessionId));
        return TokenRotationResult.succeeded(renewedSession);
    }

    @Override
    public synchronized void revoke(RefreshTokenSessionId sessionId, Instant revokedAt) {
        RefreshTokenSessionId nonNullSessionId = Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(revokedAt, "revokedAt");
        StoredSession storedSession = sessions.get(nonNullSessionId);
        if (storedSession != null) {
            storedSession.revoked = true;
        }
    }

    @Override
    public synchronized void revoke(TokenDigest<RefreshToken> digest, Instant revokedAt) {
        TokenDigest<RefreshToken> nonNullDigest = Objects.requireNonNull(digest, "digest");
        Objects.requireNonNull(revokedAt, "revokedAt");
        StoredDigest storedDigest = digests.get(nonNullDigest);
        if (storedDigest != null) {
            requireStoredSession(storedDigest.sessionId).revoked = true;
        }
    }

    synchronized int getSessionCount() {
        return sessions.size();
    }

    synchronized int getDigestCount() {
        return digests.size();
    }

    synchronized RefreshTokenSession getSession(RefreshTokenSessionId sessionId) {
        return requireStoredSession(Objects.requireNonNull(sessionId, "sessionId")).session;
    }

    synchronized RefreshTokenSessionId getSessionId(TokenDigest<RefreshToken> digest) {
        StoredDigest storedDigest = digests.get(Objects.requireNonNull(digest, "digest"));
        if (storedDigest == null) {
            throw new IllegalArgumentException("Unknown Refresh Token Digest");
        }
        return storedDigest.sessionId;
    }

    synchronized boolean isConsumed(TokenDigest<RefreshToken> digest) {
        StoredDigest storedDigest = digests.get(Objects.requireNonNull(digest, "digest"));
        return storedDigest != null && storedDigest.consumed;
    }

    synchronized boolean isRevoked(RefreshTokenSessionId sessionId) {
        StoredSession storedSession = sessions.get(Objects.requireNonNull(sessionId, "sessionId"));
        return storedSession != null && storedSession.revoked;
    }

    private StoredSession requireStoredSession(RefreshTokenSessionId sessionId) {
        StoredSession storedSession = sessions.get(sessionId);
        if (storedSession == null) {
            throw new IllegalStateException("Refresh Token Digest has no Session");
        }
        return storedSession;
    }

    private static final class StoredSession {
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
