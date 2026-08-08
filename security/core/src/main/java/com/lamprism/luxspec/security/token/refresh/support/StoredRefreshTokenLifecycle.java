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

package com.lamprism.luxspec.security.token.refresh.support;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.token.IssuedToken;
import com.lamprism.luxspec.security.token.SessionLifetime;
import com.lamprism.luxspec.security.token.Token;
import com.lamprism.luxspec.security.token.TokenHasher;
import com.lamprism.luxspec.security.token.TokenIssuance;
import com.lamprism.luxspec.security.token.TokenIssuer;
import com.lamprism.luxspec.security.token.TokenRefresher;
import com.lamprism.luxspec.security.token.TokenRotation;
import com.lamprism.luxspec.security.token.TokenRotationResult;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.refresh.RefreshToken;
import com.lamprism.luxspec.security.token.refresh.RefreshTokenAuthenticationResolver;
import com.lamprism.luxspec.security.token.refresh.RefreshTokenSession;
import com.lamprism.luxspec.security.token.refresh.RefreshTokenSessionFactory;
import com.lamprism.luxspec.security.token.refresh.RefreshTokenSessionId;
import com.lamprism.luxspec.security.token.refresh.RefreshTokenSessionStore;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates a store-backed opaque Refresh Token lifecycle.
 *
 * @param <S> the application Refresh Token Session type
 * @author RollW
 */
public final class StoredRefreshTokenLifecycle<S extends RefreshTokenSession>
        implements TokenIssuer, TokenRefresher {
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int SESSION_ID_BYTE_LENGTH = 16;

    private final RefreshTokenSessionStore<S> sessionStore;
    private final TokenIssuer accessTokenIssuer;
    private final TokenHasher<RefreshToken> tokenHasher;
    private final RefreshTokenSessionFactory<S> sessionFactory;
    private final RefreshTokenAuthenticationResolver<S> authenticationResolver;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Duration idleTimeout;
    private final Duration maximumLifetime;

    /**
     * Creates a store-backed Refresh Token lifecycle.
     *
     * @param sessionStore           the authoritative Refresh Token Session store
     * @param accessTokenIssuer      the issuer used for access-side Tokens
     * @param tokenHasher            the one-way Refresh Token hasher
     * @param sessionFactory         the application Session projection factory
     * @param authenticationResolver the current Authentication resolver
     * @param secureRandom           the cryptographically secure random source
     * @param clock                  the lifecycle clock
     * @param idleTimeout            the positive renewable idle timeout
     * @param maximumLifetime        the positive non-renewable maximum lifetime
     */
    public StoredRefreshTokenLifecycle(
            RefreshTokenSessionStore<S> sessionStore,
            TokenIssuer accessTokenIssuer,
            TokenHasher<RefreshToken> tokenHasher,
            RefreshTokenSessionFactory<S> sessionFactory,
            RefreshTokenAuthenticationResolver<S> authenticationResolver,
            SecureRandom secureRandom,
            Clock clock,
            Duration idleTimeout,
            Duration maximumLifetime
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer, "accessTokenIssuer");
        this.tokenHasher = Objects.requireNonNull(tokenHasher, "tokenHasher");
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory");
        this.authenticationResolver = Objects.requireNonNull(authenticationResolver, "authenticationResolver");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idleTimeout = requirePositive(idleTimeout, "idleTimeout");
        this.maximumLifetime = requirePositive(maximumLifetime, "maximumLifetime");
    }

    @Override
    public TokenIssuance issue(Authentication authentication) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        Instant issuedAt = clock.instant();
        RefreshToken refreshToken = createRefreshToken();
        SessionLifetime lifetime = SessionLifetime.start(issuedAt, idleTimeout, maximumLifetime);
        S session = createSession(nonNullAuthentication, lifetime);
        TokenIssuance accessIssuance = issueAccessTokens(nonNullAuthentication);
        TokenIssuance completeIssuance = appendRefreshToken(
                accessIssuance,
                refreshToken,
                issuedAt,
                lifetime.getIdleExpiresAt()
        );
        sessionStore.create(session, tokenHasher.hash(refreshToken));
        return completeIssuance;
    }

    @Override
    public TokenIssuance refresh(RefreshToken refreshToken) {
        RefreshToken nonNullRefreshToken = Objects.requireNonNull(refreshToken, "refreshToken");
        Instant rotatedAt = clock.instant();
        RefreshToken successor = createRefreshToken();
        TokenRotation<RefreshToken> rotation = new TokenRotation<>(
                tokenHasher.hash(nonNullRefreshToken),
                tokenHasher.hash(successor),
                rotatedAt
        );
        TokenRotationResult<S> result = sessionStore.rotate(rotation);
        if (result instanceof TokenRotationResult.Rejected<S>) {
            throw rejected();
        }
        TokenRotationResult.Succeeded<S> succeeded = (TokenRotationResult.Succeeded<S>) result;
        S session = succeeded.getState();
        try {
            return issueSuccessor(session, successor, rotatedAt);
        } catch (RuntimeException | Error failure) {
            revokeAfterFailure(session, rotatedAt, failure);
            throw failure;
        }
    }

    private TokenIssuance issueSuccessor(S session, RefreshToken successor, Instant issuedAt) {
        S nonNullSession = Objects.requireNonNull(session, "session");
        if (nonNullSession.getLifetime().isExpiredAt(issuedAt)) {
            throw new IllegalStateException("Refresh Token Store returned an expired Session");
        }
        Authentication currentAuthentication = Objects.requireNonNull(
                authenticationResolver.resolve(nonNullSession),
                "authentication"
        );
        requireSameSubject(nonNullSession.getSubject(), currentAuthentication.subject());
        Authentication boundedAuthentication = new Authentication(
                currentAuthentication.subject(),
                currentAuthentication.grants().intersect(nonNullSession.getAuthorizationCeiling())
        );
        TokenIssuance accessIssuance = issueAccessTokens(boundedAuthentication);
        return appendRefreshToken(
                accessIssuance,
                successor,
                issuedAt,
                nonNullSession.getLifetime().getIdleExpiresAt()
        );
    }

    private TokenIssuance issueAccessTokens(Authentication authentication) {
        TokenIssuance issuance = Objects.requireNonNull(
                accessTokenIssuer.issue(authentication),
                "accessTokenIssuance"
        );
        issuance.require(AccessToken.KIND);
        return issuance;
    }

    private TokenIssuance appendRefreshToken(
            TokenIssuance accessIssuance,
            RefreshToken refreshToken,
            Instant issuedAt,
            Instant expiresAt
    ) {
        List<IssuedToken<? extends Token>> tokens = new ArrayList<>(accessIssuance.getTokens());
        tokens.add(IssuedToken.of(refreshToken, issuedAt, expiresAt));
        return TokenIssuance.of(tokens);
    }

    private S createSession(Authentication authentication, SessionLifetime lifetime) {
        RefreshTokenSessionId sessionId = new RefreshTokenSessionId(createSecret(SESSION_ID_BYTE_LENGTH));
        S session = Objects.requireNonNull(
                sessionFactory.create(sessionId, authentication, lifetime),
                "session"
        );
        if (!sessionId.equals(session.getId())) {
            throw new IllegalArgumentException("Refresh Token Session Factory changed the Session ID");
        }
        requireSameSubject(authentication.subject(), session.getSubject());
        if (!lifetime.equals(session.getLifetime())) {
            throw new IllegalArgumentException("Refresh Token Session Factory changed the Session lifetime");
        }
        if (!session.getAuthorizationCeiling()
                .intersect(authentication.grants())
                .equals(session.getAuthorizationCeiling())) {
            throw new IllegalArgumentException("Refresh Token Session authorization exceeds Authentication grants");
        }
        return session;
    }

    private void revokeAfterFailure(S session, Instant revokedAt, Throwable failure) {
        try {
            sessionStore.revoke(session.getId(), revokedAt);
        } catch (RuntimeException | Error revocationFailure) {
            failure.addSuppressed(revocationFailure);
        }
    }

    private RefreshToken createRefreshToken() {
        return new RefreshToken(createSecret(TOKEN_BYTE_LENGTH));
    }

    private String createSecret(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void requireSameSubject(Subject expected, Subject actual) {
        Subject nonNullExpected = Objects.requireNonNull(expected, "expected");
        Subject nonNullActual = Objects.requireNonNull(actual, "actual");
        boolean sameType = nonNullExpected.getType().equals(nonNullActual.getType());
        boolean sameId = nonNullExpected.getId().equals(nonNullActual.getId());
        if (!sameType || !sameId) {
            throw new AuthenticationException(
                    AuthErrorCode.REFRESH_TOKEN_REJECTED,
                    "Refresh Token Session subject changed"
            );
        }
    }

    private static Duration requirePositive(Duration duration, String name) {
        Duration nonNullDuration = Objects.requireNonNull(duration, name);
        if (nonNullDuration.isNegative() || nonNullDuration.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return nonNullDuration;
    }

    private static AuthenticationException rejected() {
        return new AuthenticationException(
                AuthErrorCode.REFRESH_TOKEN_REJECTED,
                "Refresh Token was rejected"
        );
    }
}
