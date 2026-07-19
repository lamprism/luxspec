package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.token.SessionLifetime;

/**
 * Creates an application-specific Refresh Token Session projection.
 *
 * @param <S> the Refresh Token Session type
 * @author RollW
 */
@FunctionalInterface
public interface RefreshTokenSessionFactory<S extends RefreshTokenSession> {
    /**
     * Creates a Refresh Token Session for one authenticated actor.
     *
     * @param id the generated stable session identifier
     * @param authentication the authentication that opens the session
     * @param lifetime the configured session lifetime
     * @return the application session projection
     */
    S create(
            RefreshTokenSessionId id,
            Authentication authentication,
            SessionLifetime lifetime
    );
}
