package com.lamprism.luxspec.security.token;

import java.time.Instant;

/**
 * Revokes one token session by its typed identifier.
 *
 * @param <ID> the session identifier type
 * @author RollW
 */
@FunctionalInterface
public interface SessionRevoker<ID> {
    /**
     * Revokes one session idempotently.
     *
     * @param sessionId the typed session identifier
     * @param revokedAt the revocation time
     */
    void revoke(ID sessionId, Instant revokedAt);
}
