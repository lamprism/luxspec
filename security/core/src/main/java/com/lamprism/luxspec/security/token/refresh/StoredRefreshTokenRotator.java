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

import com.lamprism.luxspec.security.token.TokenHasher;
import com.lamprism.luxspec.security.token.TokenRotator;

import java.time.Instant;
import java.util.Objects;

/**
 * Rotates refresh-token digests through an authoritative session store.
 *
 * @param <S> the application session type
 * @author RollW
 */
public final class StoredRefreshTokenRotator<S extends RefreshTokenSession>
        implements TokenRotator<RefreshToken, S> {
    private final RefreshTokenSessionStore<S> store;
    private final TokenHasher<RefreshToken> hasher;

    /**
     * Creates a store-backed token rotator.
     *
     * @param store  the authoritative session store
     * @param hasher the refresh-token digest strategy
     */
    public StoredRefreshTokenRotator(RefreshTokenSessionStore<S> store, TokenHasher<RefreshToken> hasher) {
        this.store = Objects.requireNonNull(store, "store");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
    }

    @Override
    public S rotate(RefreshToken presented, RefreshToken successor, Instant rotatedAt) {
        return store.rotate(hasher.hash(presented), hasher.hash(successor), rotatedAt);
    }
}
