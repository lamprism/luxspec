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
import java.util.function.Function;

/**
 * Converts Servlet request facts into the provider-independent ingress model.
 *
 * <p>The default address source is {@link HttpServletRequest#getRemoteAddr()}. Applications using
 * a trusted proxy may supply an explicit resolver instead of accepting forwarded headers here.</p>
 *
 * @author RollW
 */
public class ServletIngressRequestAdapter {
    private final Function<HttpServletRequest, String> clientAddressResolver;

    /**
     * Creates an adapter using the Servlet remote address.
     */
    public ServletIngressRequestAdapter() {
        this(HttpServletRequest::getRemoteAddr);
    }

    /**
     * Creates an adapter with an explicit client-address resolver.
     *
     * @param clientAddressResolver the trusted client-address resolver
     */
    public ServletIngressRequestAdapter(Function<HttpServletRequest, String> clientAddressResolver) {
        this.clientAddressResolver = Objects.requireNonNull(clientAddressResolver, "clientAddressResolver");
    }

    /**
     * Converts one Servlet request into immutable ingress facts.
     *
     * @param request the current Servlet request
     * @return the provider-independent ingress request
     */
    public IngressRequest adapt(HttpServletRequest request) {
        HttpServletRequest nonNullRequest = Objects.requireNonNull(request, "request");
        return new IngressRequest(
                nonNullRequest.getMethod(),
                nonNullRequest.getRequestURI(),
                clientAddressResolver.apply(nonNullRequest)
        );
    }
}
