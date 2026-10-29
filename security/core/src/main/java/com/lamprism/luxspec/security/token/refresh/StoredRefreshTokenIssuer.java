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

import com.lamprism.luxspec.context.ContextKey;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.token.IssuedToken;
import com.lamprism.luxspec.security.token.TokenGenerator;
import com.lamprism.luxspec.security.token.TokenHasher;
import com.lamprism.luxspec.security.token.TokenIssuer;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Issues an opaque refresh token and registers its digest with the supplied session.
 *
 * @param <S> the application session type
 * @author RollW
 */
public final class StoredRefreshTokenIssuer<S extends RefreshTokenSession> implements TokenIssuer<RefreshToken> {
    private final RefreshTokenSessionStore<S> store;
    private final TokenGenerator<RefreshToken> generator;
    private final TokenHasher<RefreshToken> hasher;
    private final ContextKey<S> sessionKey;
    private final Clock clock;

    /**
     * Creates an issuer for an explicit session attribute and storage policy.
     *
     * @param store      the authoritative session store
     * @param generator  the refresh-token generator
     * @param hasher     the refresh-token digest strategy
     * @param sessionKey the issuance session attribute key
     * @param clock      the issuance clock
     */
    public StoredRefreshTokenIssuer(
            RefreshTokenSessionStore<S> store,
            TokenGenerator<RefreshToken> generator,
            TokenHasher<RefreshToken> hasher,
            ContextKey<S> sessionKey,
            Clock clock
    ) {
        this.store = Objects.requireNonNull(store, "store");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.sessionKey = Objects.requireNonNull(sessionKey, "sessionKey");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public IssuedToken<RefreshToken> issue(Authentication authentication, ExecutionContext attributes) {
        Authentication actor = Objects.requireNonNull(authentication, "authentication");
        S session = Objects.requireNonNull(attributes, "attributes").get(sessionKey)
                .orElseThrow(() -> new IllegalArgumentException("Refresh Token Session is missing"));
        if (!SimpleSubject.from(session.getSubject()).equals(SimpleSubject.from(actor.subject()))) {
            throw new IllegalArgumentException("Refresh Token Session subject changed");
        }
        if (!session.getAuthorizationCeiling().intersect(actor.grants())
                .equals(session.getAuthorizationCeiling())) {
            throw new IllegalArgumentException("Refresh Token Session authorization exceeds Authentication grants");
        }
        Instant issuedAt = clock.instant();
        if (issuedAt.isBefore(session.getLifetime().getCreatedAt()) || session.getLifetime().isExpiredAt(issuedAt)) {
            throw new IllegalArgumentException("Refresh Token Session is not active at issuance");
        }
        RefreshToken token = Objects.requireNonNull(generator.generate(), "generated token");
        IssuedToken<RefreshToken> issued = IssuedToken.of(token, issuedAt, session.getLifetime().getIdleExpiresAt());
        store.create(session, hasher.hash(token));
        return issued;
    }
}
