package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.security.token.refresh.RefreshToken;

/**
 * Refreshes a token issuance through one Refresh Token.
 *
 * @author RollW
 */
@FunctionalInterface
public interface TokenRefresher {
    /**
     * Rotates a Refresh Token and issues its configured successor Tokens.
     *
     * @param refreshToken the presented Refresh Token
     * @return the successor Token issuance
     */
    TokenIssuance refresh(RefreshToken refreshToken);
}
