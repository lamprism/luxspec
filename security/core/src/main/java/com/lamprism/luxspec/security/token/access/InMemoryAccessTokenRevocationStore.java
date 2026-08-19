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

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keeps access-token revocations in memory until the corresponding token expires.
 *
 * <p>Only the verified token identifier and expiration are retained. Expired entries are removed
 * opportunistically during lookups and can also be reclaimed in batches with {@link #cleanup()}.
 * This implementation is suitable for one JVM and must not be used where revocation state must
 * survive restarts or be shared across nodes.</p>
 *
 * @author RollW
 */
public class InMemoryAccessTokenRevocationStore implements AccessTokenRevocationStore {
    private final Clock clock;
    private final ConcurrentMap<String, Instant> revokedUntil = new ConcurrentHashMap<>();

    /**
     * Creates a store using the UTC system clock.
     */
    public InMemoryAccessTokenRevocationStore() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a store using an explicit clock.
     *
     * @param clock the clock used for expiration checks
     */
    public InMemoryAccessTokenRevocationStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void revoke(VerifiedAccessToken accessToken) {
        VerifiedAccessToken nonNullAccessToken = Objects.requireNonNull(accessToken, "accessToken");
        Instant now = now();
        Instant expiresAt = nonNullAccessToken.getExpiresAt();
        if (!expiresAt.isAfter(now)) {
            return;
        }
        revokedUntil.merge(
                nonNullAccessToken.getTokenId(),
                expiresAt,
                InMemoryAccessTokenRevocationStore::laterExpiration
        );
    }

    @Override
    public boolean isRevoked(VerifiedAccessToken accessToken) {
        VerifiedAccessToken nonNullAccessToken = Objects.requireNonNull(accessToken, "accessToken");
        String tokenId = nonNullAccessToken.getTokenId();
        Instant now = now();
        Instant revokedExpiry = revokedUntil.get(tokenId);
        if (revokedExpiry == null) {
            return false;
        }
        if (!nonNullAccessToken.getExpiresAt().isAfter(now)) {
            if (!revokedExpiry.isAfter(now)) {
                revokedUntil.remove(tokenId, revokedExpiry);
            }
            return false;
        }
        if (!revokedExpiry.isAfter(now)) {
            revokedUntil.remove(tokenId, revokedExpiry);
            return false;
        }
        return true;
    }

    /**
     * Removes every revocation whose retention period has ended.
     *
     * @return the number of removed entries
     */
    public int cleanup() {
        Instant now = now();
        int removed = 0;
        for (Map.Entry<String, Instant> entry : revokedUntil.entrySet()) {
            if (!entry.getValue().isAfter(now) && revokedUntil.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * Returns the number of retained revocations, including entries awaiting cleanup.
     *
     * @return the retained entry count
     */
    public int size() {
        return revokedUntil.size();
    }

    private Instant now() {
        return Objects.requireNonNull(clock.instant(), "clock instant");
    }

    private static Instant laterExpiration(Instant first, Instant second) {
        return first.isAfter(second) ? first : second;
    }
}
