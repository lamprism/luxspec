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

package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.security.token.TokenVerifier;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.NoOpAccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Converts a verified access token into the unified Authentication model.
 *
 * @author RollW
 */
public final class AccessTokenAuthenticator implements Authenticator<AccessTokenCredentials> {
    private static final CredentialType<AccessTokenCredentials> CREDENTIAL_TYPE = CredentialType.of(
            "access-token",
            AccessTokenCredentials.class
    );
    private final TokenVerifier<AccessToken, VerifiedAccessToken> tokenVerifier;
    private final SubjectResolver subjectResolver;
    private final AccessTokenRevocationStore revocationStore;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates an authenticator with Token verification and current-subject resolution roles.
     *
     * @param tokenVerifier   the authoritative Access Token verifier
     * @param subjectResolver the current subject and state resolver
     */
    public AccessTokenAuthenticator(
            TokenVerifier<AccessToken, VerifiedAccessToken> tokenVerifier,
            SubjectResolver subjectResolver
    ) {
        this(tokenVerifier, subjectResolver, NoOpAccessTokenRevocationStore.getInstance());
    }

    /**
     * Creates an authenticator with an explicit optional access-token revocation store.
     *
     * @param tokenVerifier   the authoritative Access Token verifier
     * @param subjectResolver the current subject and state resolver
     * @param revocationStore the optional verified-token revocation store
     */
    public AccessTokenAuthenticator(
            TokenVerifier<AccessToken, VerifiedAccessToken> tokenVerifier,
            SubjectResolver subjectResolver,
            AccessTokenRevocationStore revocationStore
    ) {
        this(tokenVerifier, subjectResolver, revocationStore, event -> {
        }, Clock.systemUTC());
    }

    /**
     * Creates an access-token authenticator with explicit security event publication.
     *
     * @param tokenVerifier   the authoritative Access Token verifier
     * @param subjectResolver the current subject and state resolver
     * @param revocationStore the optional verified-token revocation store
     * @param eventPublisher  the authentication event publisher
     * @param clock           the authentication event timestamp clock
     */
    public AccessTokenAuthenticator(
            TokenVerifier<AccessToken, VerifiedAccessToken> tokenVerifier,
            SubjectResolver subjectResolver,
            AccessTokenRevocationStore revocationStore,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier, "tokenVerifier");
        this.subjectResolver = Objects.requireNonNull(subjectResolver, "subjectResolver");
        this.revocationStore = Objects.requireNonNull(revocationStore, "revocationStore");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Returns the credential type handled by this authenticator.
     *
     * @return the access-token credential type
     */
    @Override
    public CredentialType<AccessTokenCredentials> getCredentialType() {
        return CREDENTIAL_TYPE;
    }

    /**
     * Verifies the token, applies optional revocation state, and resolves the current subject.
     *
     * @param credentials the opaque access-token credentials
     * @return the reconstructed authentication
     * @throws AuthenticationException when a verified token has been revoked
     */
    @Override
    public Authentication authenticate(AccessTokenCredentials credentials) {
        Instant startedAt = clock.instant();
        Authentication authentication;
        try {
            authentication = authenticateInternal(credentials);
        } catch (AuthenticationException failure) {
            Instant completedAt = clock.instant();
            eventPublisher.publish(AuthenticationEvent.failed(
                    CREDENTIAL_TYPE.getName(),
                    failure.getErrorCode(),
                    completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
            throw failure;
        } catch (RuntimeException failure) {
            Instant completedAt = clock.instant();
            eventPublisher.publish(AuthenticationEvent.failed(
                    CREDENTIAL_TYPE.getName(),
                    AuthErrorCode.AUTHENTICATION_FAILURE,
                    completedAt,
                    elapsedSince(startedAt, completedAt)
            ));
            throw failure;
        }
        Instant completedAt = clock.instant();
        eventPublisher.publish(AuthenticationEvent.succeeded(
                CREDENTIAL_TYPE.getName(),
                authentication,
                completedAt,
                elapsedSince(startedAt, completedAt)
        ));
        return authentication;
    }

    private Authentication authenticateInternal(AccessTokenCredentials credentials) {
        VerifiedAccessToken token = tokenVerifier.verify(
                Objects.requireNonNull(credentials, "credentials").getAccessToken()
        );
        if (revocationStore.isRevoked(token)) {
            throw new AuthenticationException(AuthErrorCode.ACCESS_TOKEN_REVOKED, "Access token was rejected");
        }
        if (SystemSubject.TYPE.equals(token.getSubjectType())) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "System subject tokens are not accepted");
        }
        Subject subject = subjectResolver.resolve(token.getSubjectType(), token.getSubjectId());
        return new Authentication(subject, token.getGrants());
    }

    private static Duration elapsedSince(Instant startedAt, Instant completedAt) {
        Duration elapsed = Duration.between(startedAt, completedAt);
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }
}
