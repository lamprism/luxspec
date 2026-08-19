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

import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAccessTokenRevocationStoreTest {
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");

    @Test
    void expiresAndReclaimsRevocationState() {
        MutableClock clock = new MutableClock(NOW, ZoneOffset.UTC);
        InMemoryAccessTokenRevocationStore store = new InMemoryAccessTokenRevocationStore(clock);
        VerifiedAccessToken token = token("token-1", NOW.plusSeconds(30L));

        store.revoke(token);

        assertTrue(store.isRevoked(token));
        clock.setInstant(NOW.plusSeconds(30L));
        assertFalse(store.isRevoked(token));
        assertEquals(0, store.size());
    }

    @Test
    void ignoresAnAlreadyExpiredTokenWithoutRemovingAnActiveRevocation() {
        InMemoryAccessTokenRevocationStore store = new InMemoryAccessTokenRevocationStore(
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        VerifiedAccessToken active = token("token-1", NOW.plusSeconds(30L));
        VerifiedAccessToken expired = token("token-1", NOW);

        store.revoke(active);
        store.revoke(expired);

        assertTrue(store.isRevoked(active));
        assertEquals(1, store.size());
    }

    private static VerifiedAccessToken token(String tokenId, Instant expiresAt) {
        return VerifiedAccessToken.of(
                "user",
                "42",
                AuthorizationGrantSet.of(List.of()),
                tokenId,
                expiresAt.minusSeconds(60L),
                expiresAt
        );
    }

    private static final class MutableClock extends Clock {
        private volatile Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = Objects.requireNonNull(instant, "instant");
            this.zone = Objects.requireNonNull(zone, "zone");
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void setInstant(Instant instant) {
            this.instant = Objects.requireNonNull(instant, "instant");
        }
    }
}
