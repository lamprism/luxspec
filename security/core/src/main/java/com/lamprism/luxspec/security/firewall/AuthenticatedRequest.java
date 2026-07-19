package com.lamprism.luxspec.security.firewall;

import com.lamprism.luxspec.security.authentication.Authentication;
import java.util.Objects;

/**
 * Combines immutable ingress facts with one authenticated actor for post-authentication policy.
 *
 * @author RollW
 */
public final class AuthenticatedRequest {
    private final IngressRequest ingressRequest;
    private final Authentication authentication;

    /**
     * Creates post-authentication firewall facts.
     *
     * @param ingressRequest the original ingress facts
     * @param authentication the non-null authenticated actor
     */
    public AuthenticatedRequest(IngressRequest ingressRequest, Authentication authentication) {
        this.ingressRequest = Objects.requireNonNull(ingressRequest, "ingressRequest");
        this.authentication = Objects.requireNonNull(authentication, "authentication");
    }

    /**
     * Returns the original ingress facts.
     *
     * @return the ingress request
     */
    public IngressRequest getIngressRequest() {
        return ingressRequest;
    }

    /**
     * Returns the authenticated actor.
     *
     * @return the authentication
     */
    public Authentication getAuthentication() {
        return authentication;
    }
}
