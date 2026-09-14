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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Holds immutable idle and absolute lifetime bounds for a token session.
 *
 * @author RollW
 */
public final class SessionLifetime {
    private final Instant createdAt;
    private final Instant lastActivityAt;
    private final Duration idleTimeout;
    private final Instant idleExpiresAt;
    private final Instant absoluteExpiresAt;

    /**
     * Creates a session lifetime from explicit creation, activity, idle, and absolute bounds.
     *
     * @param createdAt         the session creation time
     * @param lastActivityAt    the latest accepted activity time
     * @param idleTimeout       the positive renewable idle timeout
     * @param absoluteExpiresAt the non-renewable absolute expiration time
     */
    public SessionLifetime(
            Instant createdAt,
            Instant lastActivityAt,
            Duration idleTimeout,
            Instant absoluteExpiresAt
    ) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt");
        this.idleTimeout = requirePositive(idleTimeout, "idleTimeout");
        this.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt");
        if (this.lastActivityAt.isBefore(this.createdAt)) {
            throw new IllegalArgumentException("Session activity must not precede creation");
        }
        if (!this.absoluteExpiresAt.isAfter(this.createdAt)) {
            throw new IllegalArgumentException("Absolute expiration must follow session creation");
        }
        if (!this.lastActivityAt.isBefore(this.absoluteExpiresAt)) {
            throw new IllegalArgumentException("Session activity must precede absolute expiration");
        }
        this.idleExpiresAt = earliest(this.lastActivityAt.plus(this.idleTimeout), this.absoluteExpiresAt);
    }

    /**
     * Starts a new session lifetime at one creation time.
     *
     * @param createdAt       the session creation and initial activity time
     * @param idleTimeout     the positive renewable idle timeout
     * @param maximumLifetime the positive non-renewable maximum lifetime
     * @return the new session lifetime
     */
    public static SessionLifetime start(
            Instant createdAt,
            Duration idleTimeout,
            Duration maximumLifetime
    ) {
        Instant nonNullCreatedAt = Objects.requireNonNull(createdAt, "createdAt");
        Duration positiveMaximum = requirePositive(maximumLifetime, "maximumLifetime");
        return new SessionLifetime(
                nonNullCreatedAt,
                nonNullCreatedAt,
                idleTimeout,
                nonNullCreatedAt.plus(positiveMaximum)
        );
    }

    /**
     * Advances activity and idle expiration without extending absolute expiration.
     *
     * @param activityAt the new activity time
     * @return the renewed immutable lifetime, or this value when activity is unchanged
     * @throws IllegalArgumentException when activity moves backward
     * @throws IllegalStateException    when the session has already expired
     */
    public SessionLifetime renew(Instant activityAt) {
        Instant nonNullActivityAt = Objects.requireNonNull(activityAt, "activityAt");
        if (nonNullActivityAt.isBefore(lastActivityAt)) {
            throw new IllegalArgumentException("Session activity time must not move backward");
        }
        if (isExpiredAt(nonNullActivityAt)) {
            throw new IllegalStateException("Expired session lifetime cannot be renewed");
        }
        if (nonNullActivityAt.equals(lastActivityAt)) {
            return this;
        }
        return new SessionLifetime(createdAt, nonNullActivityAt, idleTimeout, absoluteExpiresAt);
    }

    /**
     * Reports whether the idle or absolute expiration has been reached.
     *
     * @param instant the time to evaluate
     * @return whether the session is expired at that time
     */
    public boolean isExpiredAt(Instant instant) {
        Instant nonNullInstant = Objects.requireNonNull(instant, "instant");
        return !nonNullInstant.isBefore(idleExpiresAt) || !nonNullInstant.isBefore(absoluteExpiresAt);
    }

    /**
     * Returns the session creation time.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the latest accepted activity time.
     *
     * @return the last activity time
     */
    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    /**
     * Returns the renewable idle timeout.
     *
     * @return the idle timeout
     */
    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Returns the effective idle expiration capped by absolute expiration.
     *
     * @return the idle expiration time
     */
    public Instant getIdleExpiresAt() {
        return idleExpiresAt;
    }

    /**
     * Returns the non-renewable absolute expiration.
     *
     * @return the absolute expiration time
     */
    public Instant getAbsoluteExpiresAt() {
        return absoluteExpiresAt;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SessionLifetime lifetime)) {
            return false;
        }
        return createdAt.equals(lifetime.createdAt)
                && lastActivityAt.equals(lifetime.lastActivityAt)
                && idleTimeout.equals(lifetime.idleTimeout)
                && absoluteExpiresAt.equals(lifetime.absoluteExpiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, lastActivityAt, idleTimeout, absoluteExpiresAt);
    }

    private static Duration requirePositive(Duration duration, String name) {
        Duration nonNullDuration = Objects.requireNonNull(duration, name);
        if (nonNullDuration.isNegative() || nonNullDuration.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return nonNullDuration;
    }

    private static Instant earliest(Instant first, Instant second) {
        if (first.isBefore(second)) {
            return first;
        }
        return second;
    }
}
