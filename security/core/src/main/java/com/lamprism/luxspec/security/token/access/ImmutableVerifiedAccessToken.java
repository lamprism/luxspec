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
