package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.security.token.access.AccessToken;
import java.util.Objects;

/**
 * Carries one Access Token only for the duration of authentication.
 *
 * @author RollW
 */
public final class AccessTokenCredentials implements Credentials {
    private final AccessToken accessToken;

    /**
     * Creates credentials from one opaque Access Token.
     *
     * @param accessToken the presented Token
     */
    public AccessTokenCredentials(AccessToken accessToken) {
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
    }

    /**
     * Returns the token for an access-token authenticator.
     *
     * @return the opaque Access Token
     */
    public AccessToken getAccessToken() {
        return accessToken;
    }
}
