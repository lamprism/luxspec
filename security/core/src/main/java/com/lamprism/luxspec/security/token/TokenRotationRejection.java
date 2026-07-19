package com.lamprism.luxspec.security.token;

/**
 * Identifies why one token rotation was rejected.
 *
 * @author RollW
 */
public enum TokenRotationRejection {
    /**
     * The presented Digest was already consumed.
     */
    REPLAY_DETECTED,
    /**
     * The owning Token Session has expired.
     */
    EXPIRED,
    /**
     * The owning Token Session was revoked.
     */
    REVOKED,
    /**
     * The presented Digest is not known to the authoritative Store.
     */
    UNKNOWN
}
