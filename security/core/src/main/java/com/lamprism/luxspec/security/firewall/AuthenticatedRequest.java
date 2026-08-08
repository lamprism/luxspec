/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
