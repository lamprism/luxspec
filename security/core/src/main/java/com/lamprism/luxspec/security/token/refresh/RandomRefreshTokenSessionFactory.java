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

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.token.SessionLifetime;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

/**
 * Creates standard refresh sessions with secure random identifiers.
 *
 * @author RollW
 */
public final class RandomRefreshTokenSessionFactory implements RefreshTokenSessionFactory<RefreshTokenSession> {
    private final SecureRandom random;
    private final Clock clock;
    private final Duration idleTimeout;
    private final Duration maximumLifetime;

    /**
     * Creates a standard session factory.
     *
     * @param random          the secure random source
     * @param clock           the session creation clock
     * @param idleTimeout     the positive renewable idle timeout
     * @param maximumLifetime the positive absolute lifetime
     */
    public RandomRefreshTokenSessionFactory(
            SecureRandom random, Clock clock, Duration idleTimeout, Duration maximumLifetime
    ) {
        this.random = Objects.requireNonNull(random, "random");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idleTimeout = requirePositive(idleTimeout, "idleTimeout");
        this.maximumLifetime = requirePositive(maximumLifetime, "maximumLifetime");
    }

    @Override
    public RefreshTokenSession create(Authentication authentication, ExecutionContext attributes) {
        Authentication actor = Objects.requireNonNull(authentication, "authentication");
        Objects.requireNonNull(attributes, "attributes");
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        RefreshTokenSessionId id = new RefreshTokenSessionId(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        return RefreshTokenSession.of(
                id, actor.subject(), actor.grants(),
                SessionLifetime.start(clock.instant(), idleTimeout, maximumLifetime)
        );
    }

    private static Duration requirePositive(Duration value, String name) {
        Duration duration = Objects.requireNonNull(value, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }
}
