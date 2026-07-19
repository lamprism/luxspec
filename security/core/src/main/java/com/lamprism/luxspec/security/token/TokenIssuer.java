package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.security.authentication.Authentication;

/**
 * Issues authentication tokens for an already authenticated actor.
 *
 * @author RollW
 */
@FunctionalInterface
public interface TokenIssuer {
    /**
     * Issues the tokens configured for the current authentication flow.
     *
     * @param authentication the current authenticated actor
     * @return the token issuance
     */
    TokenIssuance issue(Authentication authentication);
}
