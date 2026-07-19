package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.SessionLifetime;
import java.util.Objects;

final class ImmutableRefreshTokenSession implements RefreshTokenSession {
    private final RefreshTokenSessionId id;
    private final Subject subject;
    private final AuthorizationGrantSet authorizationCeiling;
    private final SessionLifetime lifetime;

    ImmutableRefreshTokenSession(
            RefreshTokenSessionId id,
            Subject subject,
            AuthorizationGrantSet authorizationCeiling,
            SessionLifetime lifetime
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.authorizationCeiling = Objects.requireNonNull(authorizationCeiling, "authorizationCeiling");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
    }

    @Override
    public RefreshTokenSessionId getId() {
        return id;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public AuthorizationGrantSet getAuthorizationCeiling() {
        return authorizationCeiling;
    }

    @Override
    public SessionLifetime getLifetime() {
        return lifetime;
    }
}
