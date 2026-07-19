package com.lamprism.luxspec.security.token;

import java.time.Instant;
import java.util.Objects;

final class ImmutableIssuedToken<T extends Token> implements IssuedToken<T> {
    private final T token;
    private final Instant issuedAt;
    private final Instant expiresAt;

    ImmutableIssuedToken(T token, Instant issuedAt, Instant expiresAt) {
        this.token = Objects.requireNonNull(token, "token");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!this.expiresAt.isAfter(this.issuedAt)) {
            throw new IllegalArgumentException("Token expiration must follow issuance");
        }
    }

    @Override
    public T getToken() {
        return token;
    }

    @Override
    public Instant getIssuedAt() {
        return issuedAt;
    }

    @Override
    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "IssuedToken[kind=" + token.getKind().getName() + ", issuedAt=" + issuedAt
                + ", expiresAt=" + expiresAt + "]";
    }
}
