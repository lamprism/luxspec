package com.lamprism.luxspec.security.token.access;

/**
 * Stores explicit revocations for already verified access-token identities.
 *
 * <p>Implementations retain revocation state only until the token's natural expiration and
 * never receive an encoded access-token body.</p>
 *
 * @author RollW
 */
public interface AccessTokenRevocationStore {
    /**
     * Records an explicit revocation for a verified access token.
     *
     * @param accessToken the verified token metadata containing its stable identifier and expiration
     */
    void revoke(VerifiedAccessToken accessToken);

    /**
     * Reports whether a verified access token has been explicitly revoked.
     *
     * @param accessToken the verified token metadata to check
     * @return whether the token must be rejected
     */
    boolean isRevoked(VerifiedAccessToken accessToken);
}
