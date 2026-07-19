package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;

/**
 * Reconstructs current Authentication for one successfully rotated Refresh Token Session.
 *
 * @author RollW
 */
@FunctionalInterface
public interface RefreshTokenAuthenticationResolver<S extends RefreshTokenSession> {
    /**
     * Reconstructs the current authenticated actor and applies refresh-flow state policy.
     *
     * @param session the authoritative Refresh Token Session
     * @return the current authentication used to issue successor tokens
     * @throws AuthenticationException when the subject may no longer authenticate
     */
    Authentication resolve(S session);
}
