package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.SessionLifetime;
import com.lamprism.luxspec.security.token.TokenSession;

/**
 * Represents the server-authoritative renewable authorization behind rotated Refresh Tokens.
 *
 * @author RollW
 */
public interface RefreshTokenSession extends TokenSession<RefreshTokenSessionId> {
    /**
     * Creates a validated immutable Refresh Token Session.
     *
     * @param id the stable session identifier
     * @param subject the session subject
     * @param authorizationCeiling the maximum grants retained across refreshes
     * @param lifetime the idle and absolute lifetime bounds
     * @return the immutable Refresh Token Session
     */
    static RefreshTokenSession of(
            RefreshTokenSessionId id,
            Subject subject,
            AuthorizationGrantSet authorizationCeiling,
            SessionLifetime lifetime
    ) {
        return new ImmutableRefreshTokenSession(id, subject, authorizationCeiling, lifetime);
    }

    /**
     * Returns the maximum authorization retained across refreshes.
     *
     * @return the immutable authorization ceiling
     */
    AuthorizationGrantSet getAuthorizationCeiling();
}
