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

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Contributes and validates application-selected JWT access-token claims.
 *
 * <p>Policies must be thread-safe. Contributions cannot replace registered or Luxspec-owned
 * claims. Validation runs only after signature and standard claim validation have succeeded.
 * A policy must reject missing or invalid application claims when its security model requires
 * them.</p>
 *
 * @author RollW
 */
public interface JwtAccessTokenClaimsPolicy {
    /**
     * Contributes additional claims before the token is signed.
     *
     * @param authentication the authenticated actor
     * @param attributes     the explicit issuance attributes
     * @param tokenId        the generated token identifier
     * @param issuedAt       the issuance instant
     * @param expiresAt      the expiration instant
     * @param issuer         the configured issuer
     * @param audiences      the configured audiences
     * @return additional JSON-compatible claim values
     */
    default Map<String, ?> contribute(
            Authentication authentication,
            ExecutionContext attributes,
            String tokenId,
            Instant issuedAt,
            Instant expiresAt,
            String issuer,
            Set<String> audiences
    ) {
        return Map.of();
    }

    /**
     * Validates application claims from an already verified token.
     *
     * @param token  the standard verified token data
     * @param claims a read-only snapshot of the verified JWT claims
     */
    default void validate(VerifiedAccessToken token, Map<String, @Nullable Object> claims) {
    }
}
