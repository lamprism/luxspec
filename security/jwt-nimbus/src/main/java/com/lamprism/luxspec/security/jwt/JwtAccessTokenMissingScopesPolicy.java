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

package com.lamprism.luxspec.security.jwt;

import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;

import java.util.List;

/**
 * Resolves the grant ceiling of a verified JWT without a scopes claim.
 *
 * <p>The policy runs only after signature and standard identity, issuer, purpose, audience, and
 * lifetime validation. A present but malformed claim is rejected without invoking this policy.
 * An application that supplies grants for an unscoped token must establish that the issuer and
 * its token format authorize that interpretation.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface JwtAccessTokenMissingScopesPolicy {
    /**
     * Rejects tokens that do not declare scopes.
     *
     * @return the rejection policy
     */
    static JwtAccessTokenMissingScopesPolicy reject() {
        return subject -> {
            throw new IllegalArgumentException("JWT claims are missing scopes");
        };
    }

    /**
     * Treats a missing scopes claim as an empty grant ceiling.
     *
     * @return the empty-grants policy
     */
    static JwtAccessTokenMissingScopesPolicy noGrants() {
        return subject -> AuthorizationGrantSet.of(List.of());
    }

    /**
     * Determines the grant ceiling for a token without a scopes claim.
     *
     * @param subject the verified token subject
     * @return the grant ceiling
     */
    AuthorizationGrantSet resolve(Subject subject);
}
