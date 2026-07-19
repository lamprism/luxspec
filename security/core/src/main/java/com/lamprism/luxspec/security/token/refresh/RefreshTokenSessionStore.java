package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.token.SessionRevoker;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenRotation;
import com.lamprism.luxspec.security.token.TokenRotationResult;
import java.time.Instant;

/**
 * Persists Refresh Token Sessions and atomically rotates their Token Digests.
 *
 * @param <S> the Refresh Token Session type
 * @author RollW
 */
public interface RefreshTokenSessionStore<S extends RefreshTokenSession>
        extends SessionRevoker<RefreshTokenSessionId> {
    /**
     * Creates a session with its initial active Refresh Token Digest.
     *
     * @param session the new authoritative session
     * @param initialDigest the initial active Refresh Token Digest
     */
    void create(S session, TokenDigest<RefreshToken> initialDigest);

    /**
     * Atomically consumes the presented Digest and installs its successor.
     *
     * <p>Implementations must renew idle lifetime within the absolute lifetime bound. Reuse of a
     * consumed Digest must revoke the entire session in the same transaction.</p>
     *
     * @param rotation the requested Digest transition
     * @return the updated session or an authoritative rejection reason
     */
    TokenRotationResult<S> rotate(TokenRotation<RefreshToken> rotation);

    /**
     * Revokes the session associated with a presented Refresh Token Digest.
     *
     * <p>The operation is idempotent and must not reveal whether the Digest was known.</p>
     *
     * @param digest the presented Refresh Token Digest
     * @param revokedAt the revocation time
     */
    void revoke(TokenDigest<RefreshToken> digest, Instant revokedAt);
}
