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

package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.security.authentication.Authentication;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Publishes one successful issue event around an application-selected token issuer.
 *
 * <p>This decorator belongs at the outer boundary of a standalone token issuance flow. It must
 * not wrap the access-token issuer passed to {@code StoredRefreshTokenLifecycle}, because that
 * lifecycle publishes the aggregate issue and refresh events itself.</p>
 *
 * @author RollW
 */
public final class EventPublishingTokenIssuer implements TokenIssuer {
    private final TokenIssuer delegate;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates an event-publishing issuer using the UTC system clock.
     *
     * @param delegate       the token issuer being decorated
     * @param eventPublisher the token lifecycle event publisher
     */
    public EventPublishingTokenIssuer(TokenIssuer delegate, EventPublisher eventPublisher) {
        this(delegate, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates an event-publishing issuer with an explicit event clock.
     *
     * @param delegate       the token issuer being decorated
     * @param eventPublisher the token lifecycle event publisher
     * @param clock          the event timestamp clock
     */
    public EventPublishingTokenIssuer(
            TokenIssuer delegate,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public TokenIssuance issue(Authentication authentication) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        Instant startedAt = clock.instant();
        TokenIssuance issuance = Objects.requireNonNull(
                delegate.issue(nonNullAuthentication),
                "tokenIssuance"
        );
        Instant completedAt = clock.instant();
        eventPublisher.publish(TokenLifecycleEvent.issued(
                nonNullAuthentication.subject(),
                issuance,
                completedAt,
                elapsedSince(startedAt, completedAt)
        ));
        return issuance;
    }

    private static Duration elapsedSince(Instant startedAt, Instant completedAt) {
        Duration elapsed = Duration.between(startedAt, completedAt);
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }
}
