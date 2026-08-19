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
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.AccessTokenVerifier;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;

import java.util.Objects;

/**
 * Converts a verified access token into the unified Authentication model.
 *
 * @author RollW
 */
public class AccessTokenAuthenticator implements Authenticator<AccessTokenCredentials> {
    private static final CredentialType<AccessTokenCredentials> CREDENTIAL_TYPE = CredentialType.of(
            "access-token",
            AccessTokenCredentials.class
    );
    private final AccessTokenVerifier tokenVerifier;
    private final SubjectResolver subjectResolver;
    private final AccessTokenRevocationStore revocationStore;

    /**
     * Creates an authenticator with an explicit access-token revocation store.
     *
     * @param tokenVerifier   the authoritative Access Token verifier
     * @param subjectResolver the current subject and state resolver
     * @param revocationStore the verified-token revocation store
     */
    public AccessTokenAuthenticator(
            AccessTokenVerifier tokenVerifier,
            SubjectResolver subjectResolver,
            AccessTokenRevocationStore revocationStore
    ) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier, "tokenVerifier");
        this.subjectResolver = Objects.requireNonNull(subjectResolver, "subjectResolver");
        this.revocationStore = Objects.requireNonNull(revocationStore, "revocationStore");
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
     * Verifies the token, applies revocation state, and resolves the current subject.
     *
     * @param credentials the opaque access-token credentials
     * @return the reconstructed authentication
     * @throws AuthenticationException when a verified token has been revoked
     */
    @Override
    public Authentication authenticate(AccessTokenCredentials credentials) {
        VerifiedAccessToken token = tokenVerifier.verify(
                Objects.requireNonNull(credentials, "credentials").getAccessToken()
        );
        if (revocationStore.isRevoked(token)) {
            throw new AuthenticationException(AuthErrorCode.ACCESS_TOKEN_REVOKED, "Access token was rejected");
        }
        String subjectType = token.getSubjectType();
        if (SystemSubject.TYPE.equals(subjectType)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "System subject tokens are not accepted");
        }
        Subject subject = subjectResolver.resolve(subjectType, token.getSubjectId());
        return new Authentication(subject, token.getGrants());
    }
}
