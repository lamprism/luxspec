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

import java.time.Instant;
import java.util.Objects;

final class ImmutableVerifiedAccessToken implements VerifiedAccessToken {
    private final String subjectType;
    private final String subjectId;
    private final AuthorizationGrantSet grants;
    private final String tokenId;
    private final Instant issuedAt;
    private final Instant expiresAt;

    ImmutableVerifiedAccessToken(
            String subjectType,
            String subjectId,
            AuthorizationGrantSet grants,
            String tokenId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        this.subjectType = requireText(subjectType, "subjectType");
        this.subjectId = requireText(subjectId, "subjectId");
        this.grants = Objects.requireNonNull(grants, "grants");
        this.tokenId = requireText(tokenId, "tokenId");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!this.expiresAt.isAfter(this.issuedAt)) {
            throw new IllegalArgumentException("Access Token expiration must follow issuance");
        }
    }

    @Override
    public String getSubjectType() {
        return subjectType;
    }

    @Override
    public String getSubjectId() {
        return subjectId;
    }

    @Override
    public AuthorizationGrantSet getGrants() {
        return grants;
    }

    @Override
    public String getTokenId() {
        return tokenId;
    }

    @Override
    public Instant getIssuedAt() {
        return issuedAt;
    }

    @Override
    public Instant getExpiresAt() {
        return expiresAt;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
