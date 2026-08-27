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

package com.lamprism.luxspec.security.spring.firewall;

import com.lamprism.luxspec.security.firewall.IngressRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

/**
 * Converts Servlet request facts into the provider-independent ingress model.
 *
 * <p>The request URI is copied without additional path canonicalization. The servlet container,
 * routing configuration, and application firewall policy must agree on the canonical path seen
 * by both the firewall and handler mapping.</p>
 *
 * @author RollW
 */
final class ServletIngressRequestAdapter {
    private ServletIngressRequestAdapter() {
    }

    /**
     * Converts one Servlet request into immutable ingress facts.
     *
     * @param request               the current Servlet request
     * @param clientAddressResolver the trusted client-address resolver
     * @return the provider-independent ingress request
     */
    static IngressRequest adapt(
            HttpServletRequest request,
            ServletClientAddressResolver clientAddressResolver
    ) {
        HttpServletRequest nonNullRequest = Objects.requireNonNull(request, "request");
        ServletClientAddressResolver nonNullResolver = Objects.requireNonNull(
                clientAddressResolver,
                "clientAddressResolver"
        );
        return new IngressRequest(
                nonNullRequest.getMethod(),
                nonNullRequest.getRequestURI(),
                nonNullResolver.resolve(nonNullRequest)
        );
    }
}
