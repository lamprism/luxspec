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

import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;
import com.lamprism.luxspec.security.token.TokenRotation;
import com.lamprism.luxspec.security.token.TokenRotationResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Adds provider-independent revocation events to an application-owned Refresh Session store.
 *
 * <p>Rotation and creation remain delegated unchanged. Revocation events contain only the Refresh
 * Token kind and never expose a session ID, digest, or raw Refresh Token.</p>
 *
 * @param <S> the Refresh Token Session type
 * @author RollW
 */
public class EventPublishingRefreshTokenSessionStore<S extends RefreshTokenSession>
        implements RefreshTokenSessionStore<S> {
    private final RefreshTokenSessionStore<S> delegate;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a session store decorator using the UTC system clock.
     *
     * @param delegate       the application-owned session store
     * @param eventPublisher the token lifecycle event publisher
     */
    public EventPublishingRefreshTokenSessionStore(
            RefreshTokenSessionStore<S> delegate,
            EventPublisher eventPublisher
    ) {
        this(delegate, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates a session store decorator with an explicit event clock.
     *
     * @param delegate       the application-owned session store
     * @param eventPublisher the token lifecycle event publisher
     * @param clock          the event timestamp clock
     */
    public EventPublishingRefreshTokenSessionStore(
            RefreshTokenSessionStore<S> delegate,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void create(S session, TokenDigest<RefreshToken> initialDigest) {
        delegate.create(session, initialDigest);
    }

    @Override
    public TokenRotationResult<S> rotate(TokenRotation<RefreshToken> rotation) {
        return delegate.rotate(rotation);
    }

    @Override
    public void revoke(RefreshTokenSessionId sessionId, Instant revokedAt) {
        delegate.revoke(sessionId, revokedAt);
        publishRevocation(revokedAt);
    }

    @Override
    public void revoke(TokenDigest<RefreshToken> digest, Instant revokedAt) {
        delegate.revoke(digest, revokedAt);
        publishRevocation(revokedAt);
    }

    private void publishRevocation(Instant revokedAt) {
        Objects.requireNonNull(revokedAt, "revokedAt");
        eventPublisher.publish(TokenLifecycleEvent.revoked(
                RefreshToken.KIND,
                null,
                clock.instant(),
                Duration.ZERO
        ));
    }
}
