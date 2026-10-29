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

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.ErrorCodeCarrier;
import com.lamprism.luxspec.context.ContextKey;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.IssuedToken;
import com.lamprism.luxspec.security.token.SessionRevoker;
import com.lamprism.luxspec.security.token.Token;
import com.lamprism.luxspec.security.token.TokenBundleIssuer;
import com.lamprism.luxspec.security.token.TokenGenerator;
import com.lamprism.luxspec.security.token.TokenGrantPolicy;
import com.lamprism.luxspec.security.token.TokenIssuance;
import com.lamprism.luxspec.security.token.TokenIssuer;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;
import com.lamprism.luxspec.security.token.TokenRefresher;
import com.lamprism.luxspec.security.token.TokenRotationException;
import com.lamprism.luxspec.security.token.TokenRotator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Composes refresh-token issuance and rotation with selected additional token issuers.
 *
 * <p>Applications choose the session, grant, replay, event delivery, and additional issuance
 * policies. If an operation after successful session creation or rotation fails, the configured
 * session revoker is invoked before the failure is propagated. Revocation and event reporting
 * failures do not replace the original failure. A policy that affects a separate store owns its
 * consistency and transaction semantics.</p>
 *
 * @param <S> the application session type
 * @author RollW
 */
public final class RefreshTokenFlow<S extends RefreshTokenSession> implements TokenBundleIssuer, TokenRefresher {
    private final RefreshTokenSessionFactory<S> sessionFactory;
    private final ContextKey<S> sessionKey;
    private final TokenIssuer<RefreshToken> refreshIssuer;
    private final TokenGenerator<RefreshToken> generator;
    private final TokenRotator<RefreshToken, S> rotator;
    private final RefreshTokenAuthenticationResolver<S> authenticationResolver;
    private final TokenGrantPolicy grantPolicy;
    private final SessionRevoker<RefreshTokenSessionId> sessionRevoker;
    private final List<TokenIssuer<? extends Token>> issuers;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a refresh flow from explicit token and session capabilities.
     *
     * @param sessionFactory         the session creation strategy
     * @param sessionKey             the shared typed session attribute key
     * @param refreshIssuer          the refresh-token issuer
     * @param generator              the successor-token generator
     * @param rotator                the atomic refresh-token rotator
     * @param authenticationResolver the current session authentication resolver
     * @param grantPolicy            the issued and current grant policy
     * @param sessionRevoker         the session revoker used after a failed refresh
     * @param issuers                the additional token issuers in execution order
     * @param eventPublisher         the lifecycle event publisher
     * @param clock                  the operation clock
     */
    public RefreshTokenFlow(
            RefreshTokenSessionFactory<S> sessionFactory,
            ContextKey<S> sessionKey,
            TokenIssuer<RefreshToken> refreshIssuer,
            TokenGenerator<RefreshToken> generator,
            TokenRotator<RefreshToken, S> rotator,
            RefreshTokenAuthenticationResolver<S> authenticationResolver,
            TokenGrantPolicy grantPolicy,
            SessionRevoker<RefreshTokenSessionId> sessionRevoker,
            Collection<? extends TokenIssuer<? extends Token>> issuers,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory");
        this.sessionKey = Objects.requireNonNull(sessionKey, "sessionKey");
        this.refreshIssuer = Objects.requireNonNull(refreshIssuer, "refreshIssuer");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.rotator = Objects.requireNonNull(rotator, "rotator");
        this.authenticationResolver = Objects.requireNonNull(authenticationResolver, "authenticationResolver");
        this.grantPolicy = Objects.requireNonNull(grantPolicy, "grantPolicy");
        this.sessionRevoker = Objects.requireNonNull(sessionRevoker, "sessionRevoker");
        this.issuers = List.copyOf(issuers);
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public TokenIssuance issue(Authentication authentication, ExecutionContext attributes) {
        Authentication actor = Objects.requireNonNull(authentication, "authentication");
        ExecutionContext nonNullAttributes = Objects.requireNonNull(attributes, "attributes");
        Instant startedAt = clock.instant();
        S session = Objects.requireNonNull(sessionFactory.create(actor, nonNullAttributes), "session");
        validateSession(session, actor);
        ExecutionContext sessionAttributes = nonNullAttributes.with(sessionKey, session);
        List<IssuedToken<? extends Token>> issued = issueAdditional(actor, sessionAttributes);
        IssuedToken<RefreshToken> refresh = refreshIssuer.issue(actor, sessionAttributes);
        try {
            issued.add(Objects.requireNonNull(refresh, "issued refresh token"));
            TokenIssuance issuance = TokenIssuance.of(issued);
            Instant completedAt = clock.instant();
            eventPublisher.publish(TokenLifecycleEvent.issued(
                    actor.subject(), issuance, completedAt, elapsedSince(startedAt, completedAt)
            ));
            return issuance;
        } catch (RuntimeException | Error failure) {
            revokeAfterFailure(session, startedAt, failure);
            throw failure;
        }
    }

    @Override
    public TokenIssuance refresh(RefreshToken refreshToken) {
        RefreshToken presented = Objects.requireNonNull(refreshToken, "refreshToken");
        Instant startedAt = clock.instant();
        RefreshToken successor = Objects.requireNonNull(generator.generate(), "successor token");
        S session;
        try {
            session = Objects.requireNonNull(rotator.rotate(presented, successor, startedAt), "rotated session");
        } catch (TokenRotationException rejection) {
            AuthenticationException failure = rejected();
            publishRejection(startedAt, failure);
            throw failure;
        }
        try {
            TokenIssuance issuance = issueSuccessor(session, successor, startedAt);
            Instant completedAt = clock.instant();
            eventPublisher.publish(TokenLifecycleEvent.refreshed(
                    session.getSubject(), issuance, completedAt, elapsedSince(startedAt, completedAt)
            ));
            return issuance;
        } catch (RuntimeException | Error failure) {
            revokeAfterFailure(session, startedAt, failure);
            publishFailure(session, startedAt, failure);
            throw failure;
        }
    }

    private TokenIssuance issueSuccessor(S session, RefreshToken successor, Instant issuedAt) {
        if (session.getLifetime().isExpiredAt(issuedAt)) {
            throw new IllegalStateException("Refresh Token Store returned an expired Session");
        }
        Authentication current = Objects.requireNonNull(authenticationResolver.resolve(session), "authentication");
        requireSameSubject(session.getSubject(), current.subject());
        AuthorizationGrantSet effective = Objects.requireNonNull(
                grantPolicy.apply(session.getAuthorizationCeiling(), current.grants()), "effective grants"
        );
        Authentication authentication = new Authentication(current.subject(), effective);
        ExecutionContext attributes = ExecutionContext.empty().with(sessionKey, session);
        List<IssuedToken<? extends Token>> issued = issueAdditional(authentication, attributes);
        issued.add(IssuedToken.of(successor, issuedAt, session.getLifetime().getIdleExpiresAt()));
        return TokenIssuance.of(issued);
    }

    private List<IssuedToken<? extends Token>> issueAdditional(Authentication authentication, ExecutionContext attributes) {
        List<IssuedToken<? extends Token>> issued = new ArrayList<>();
        for (TokenIssuer<? extends Token> issuer : issuers) {
            issued.add(Objects.requireNonNull(issuer.issue(authentication, attributes), "issued token"));
        }
        if (!issued.isEmpty() && TokenIssuance.of(issued).find(RefreshToken.KIND).isPresent()) {
            throw new IllegalArgumentException("Additional issuers must not issue a Refresh Token");
        }
        return issued;
    }

    private static void validateSession(RefreshTokenSession session, Authentication authentication) {
        requireSameSubject(session.getSubject(), authentication.subject());
        if (!session.getAuthorizationCeiling().intersect(authentication.grants())
                .equals(session.getAuthorizationCeiling())) {
            throw new IllegalArgumentException("Refresh Token Session authorization exceeds Authentication grants");
        }
    }

    private static void requireSameSubject(Subject expected, Subject actual) {
        if (!SimpleSubject.from(expected).equals(SimpleSubject.from(actual))) {
            throw new AuthenticationException(AuthErrorCode.REFRESH_TOKEN_REJECTED, "Refresh Token Session subject changed");
        }
    }

    private void revokeAfterFailure(S session, Instant revokedAt, Throwable failure) {
        try {
            sessionRevoker.revoke(session.getId(), revokedAt);
        } catch (RuntimeException | Error revocationFailure) {
            suppress(failure, revocationFailure);
        }
    }

    private void publishRejection(Instant startedAt, AuthenticationException failure) {
        try {
            Instant completedAt = clock.instant();
            eventPublisher.publish(TokenLifecycleEvent.rejected(
                    TokenLifecycleEvent.Operation.REFRESH, null, failure.getErrorCode(), completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
        } catch (RuntimeException | Error publicationFailure) {
            suppress(failure, publicationFailure);
        }
    }

    private void publishFailure(S session, Instant startedAt, Throwable failure) {
        try {
            Instant completedAt = clock.instant();
            eventPublisher.publish(TokenLifecycleEvent.failed(
                    TokenLifecycleEvent.Operation.REFRESH, session.getSubject(), errorCode(failure), completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
        } catch (RuntimeException | Error publicationFailure) {
            suppress(failure, publicationFailure);
        }
    }

    private static void suppress(Throwable failure, Throwable secondaryFailure) {
        if (failure != secondaryFailure) {
            failure.addSuppressed(secondaryFailure);
        }
    }

    private static AuthenticationException rejected() {
        return new AuthenticationException(AuthErrorCode.REFRESH_TOKEN_REJECTED, "Refresh Token was rejected");
    }

    private static ErrorCode errorCode(Throwable failure) {
        if (failure instanceof ErrorCodeCarrier carrier) {
            return carrier.getErrorCode();
        }
        return CommonErrorCode.INTERNAL;
    }

    private static Duration elapsedSince(Instant startedAt, Instant completedAt) {
        Duration elapsed = Duration.between(startedAt, completedAt);
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }
}
