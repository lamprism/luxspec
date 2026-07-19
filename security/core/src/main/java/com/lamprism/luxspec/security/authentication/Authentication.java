package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import java.util.Objects;

/**
 * Represents one successfully authenticated actor and its effective grants.
 *
 * @author RollW
 */
public final class Authentication {
    private final Subject subject;
    private final AuthorizationGrantSet grants;

    /**
     * Creates an authenticated actor with its effective concrete grants.
     *
     * @param subject the authenticated subject identity
     * @param grants the immutable effective authorization grants
     */
    public Authentication(Subject subject, AuthorizationGrantSet grants) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.grants = Objects.requireNonNull(grants, "grants");
    }

    /**
     * Returns the authenticated subject identity.
     *
     * @return the authenticated subject
     */
    public Subject subject() {
        return subject;
    }

    /**
     * Returns immutable effective authorization grants.
     *
     * @return the effective authorization grants
     */
    public AuthorizationGrantSet grants() {
        return grants;
    }
}
