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

package com.lamprism.luxspec.security.token.access;

import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Adds provider-independent revocation events to an application-owned access-token store.
 *
 * <p>The wrapper publishes only the token kind. It never copies the verified token ID or raw
 * access-token value into the event.</p>
 *
 * @author RollW
 */
public final class EventPublishingAccessTokenRevocationStore implements AccessTokenRevocationStore {
    private final AccessTokenRevocationStore delegate;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a revocation store decorator using the UTC system clock.
     *
     * @param delegate       the application-owned revocation store
     * @param eventPublisher the token lifecycle event publisher
     */
    public EventPublishingAccessTokenRevocationStore(
            AccessTokenRevocationStore delegate,
            EventPublisher eventPublisher
    ) {
        this(delegate, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates a revocation store decorator with an explicit event clock.
     *
     * @param delegate       the application-owned revocation store
     * @param eventPublisher the token lifecycle event publisher
     * @param clock          the event timestamp clock
     */
    public EventPublishingAccessTokenRevocationStore(
            AccessTokenRevocationStore delegate,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void revoke(VerifiedAccessToken accessToken) {
        delegate.revoke(accessToken);
        eventPublisher.publish(TokenLifecycleEvent.revoked(
                AccessToken.KIND,
                null,
                clock.instant(),
                Duration.ZERO
        ));
    }

    @Override
    public boolean isRevoked(VerifiedAccessToken accessToken) {
        return delegate.isRevoked(accessToken);
    }
}
