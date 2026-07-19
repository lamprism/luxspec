package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.security.authentication.Subject;

/**
 * Represents server-authoritative lifecycle state associated with one or more tokens.
 *
 * @param <ID> the session identifier type
 * @author RollW
 */
public interface TokenSession<ID> {
    /**
     * Returns the stable typed session identifier.
     *
     * @return the session identifier
     */
    ID getId();

    /**
     * Returns the actor that owns the session.
     *
     * @return the session subject
     */
    Subject getSubject();

    /**
     * Returns the current idle and absolute lifetime bounds.
     *
     * @return the immutable session lifetime
     */
    SessionLifetime getLifetime();
}
