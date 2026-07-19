package com.lamprism.luxspec.security.token.access;

import java.util.Objects;

/**
 * Disables optional access-token revocation lookups without retaining token state.
 *
 * @author RollW
 */
public final class NoOpAccessTokenRevocationStore implements AccessTokenRevocationStore {
    private static final NoOpAccessTokenRevocationStore INSTANCE = new NoOpAccessTokenRevocationStore();

    private NoOpAccessTokenRevocationStore() {
    }

    /**
     * Returns the shared disabled revocation store.
     *
     * @return the no-op store
     */
    public static NoOpAccessTokenRevocationStore getInstance() {
        return INSTANCE;
    }

    /**
     * Accepts a revocation request without retaining state.
     *
     * @param accessToken the verified access token
     */
    @Override
    public void revoke(VerifiedAccessToken accessToken) {
        Objects.requireNonNull(accessToken, "accessToken");
    }

    /**
     * Always reports no explicit revocation.
     *
     * @param accessToken the verified access token
     * @return false because this store is disabled
     */
    @Override
    public boolean isRevoked(VerifiedAccessToken accessToken) {
        Objects.requireNonNull(accessToken, "accessToken");
        return false;
    }
}
