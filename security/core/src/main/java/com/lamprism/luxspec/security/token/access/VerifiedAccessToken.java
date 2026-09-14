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
import com.lamprism.luxspec.security.token.TokenKind;
import com.lamprism.luxspec.security.token.TokenVerification;

import java.time.Instant;

/**
 * Contains verified provider-independent Access Token claims without the raw token body.
 *
 * @author RollW
 */
public interface VerifiedAccessToken extends TokenVerification<AccessToken> {
    /**
     * Creates immutable verified Access Token claims.
     *
     * @param subjectType the stable subject category
     * @param subjectId   the stable subject identifier
     * @param grants      the effective authorization grants
     * @param tokenId     the stable token identifier used for optional revocation
     * @param issuedAt    the token issuance time
     * @param expiresAt   the exclusive token expiration time
     * @return the verified Access Token claims
     */
    static VerifiedAccessToken of(
            String subjectType,
            String subjectId,
            AuthorizationGrantSet grants,
            String tokenId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return new ImmutableVerifiedAccessToken(
                subjectType,
                subjectId,
                grants,
                tokenId,
                issuedAt,
                expiresAt
        );
    }

    @Override
    default TokenKind<AccessToken> getTokenKind() {
        return AccessToken.KIND;
    }

    /**
     * Returns the stable subject category.
     *
     * @return the subject type
     */
    String getSubjectType();

    /**
     * Returns the stable subject identifier.
     *
     * @return the subject identifier
     */
    String getSubjectId();

    /**
     * Returns the effective authorization grants encoded in the token.
     *
     * @return the effective authorization grants
     */
    AuthorizationGrantSet getGrants();

    /**
     * Returns the stable token identifier used for optional revocation.
     *
     * @return the token identifier
     */
    String getTokenId();

    /**
     * Returns the token issuance time.
     *
     * @return the issuance time
     */
    Instant getIssuedAt();

    /**
     * Returns the exclusive token expiration time.
     *
     * @return the expiration time
     */
    Instant getExpiresAt();
}
