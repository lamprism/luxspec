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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLifetimeTest {
    private static final Instant CREATED_AT = Instant.parse("2026-07-15T00:00:00Z");

    @Test
    void startsWithIndependentIdleAndAbsoluteBounds() {
        SessionLifetime lifetime = SessionLifetime.start(
                CREATED_AT,
                Duration.ofMinutes(10),
                Duration.ofHours(1)
        );

        assertEquals(CREATED_AT, lifetime.getCreatedAt());
        assertEquals(CREATED_AT, lifetime.getLastActivityAt());
        assertEquals(CREATED_AT.plus(Duration.ofMinutes(10)), lifetime.getIdleExpiresAt());
        assertEquals(CREATED_AT.plus(Duration.ofHours(1)), lifetime.getAbsoluteExpiresAt());
        assertFalse(lifetime.isExpiredAt(CREATED_AT.plus(Duration.ofMinutes(9))));
        assertTrue(lifetime.isExpiredAt(CREATED_AT.plus(Duration.ofMinutes(10))));
    }

    @Test
    void renewsIdleLifetimeWithoutExtendingAbsoluteExpiration() {
        SessionLifetime lifetime = SessionLifetime.start(
                CREATED_AT,
                Duration.ofMinutes(20),
                Duration.ofMinutes(30)
        );

        SessionLifetime renewed = lifetime.renew(CREATED_AT.plus(Duration.ofMinutes(15)));

        assertEquals(CREATED_AT.plus(Duration.ofMinutes(15)), renewed.getLastActivityAt());
        assertEquals(CREATED_AT.plus(Duration.ofMinutes(30)), renewed.getIdleExpiresAt());
        assertEquals(lifetime.getAbsoluteExpiresAt(), renewed.getAbsoluteExpiresAt());
        assertSame(renewed, renewed.renew(renewed.getLastActivityAt()));
    }

    @Test
    void rejectsBackwardOrExpiredRenewalAndInvalidDurations() {
        SessionLifetime lifetime = SessionLifetime.start(
                CREATED_AT,
                Duration.ofMinutes(10),
                Duration.ofHours(1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> lifetime.renew(CREATED_AT.minusSeconds(1))
        );
        assertThrows(
                IllegalStateException.class,
                () -> lifetime.renew(lifetime.getIdleExpiresAt())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SessionLifetime.start(CREATED_AT, Duration.ZERO, Duration.ofHours(1))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SessionLifetime.start(CREATED_AT, Duration.ofMinutes(1), Duration.ZERO)
        );
    }
}
