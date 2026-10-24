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
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;

import java.util.Objects;

/**
 * Converts Servlet request facts into the provider-independent ingress model.
 *
 * <p>The path is parsed using Spring's HTTP path rules, including context-path removal, percent
 * decoding of path segments, and removal of matrix parameters.</p>
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
                canonicalPath(nonNullRequest),
                nonNullResolver.resolve(nonNullRequest)
        );
    }

    private static String canonicalPath(HttpServletRequest request) {
        try {
            PathContainer path = RequestPath.parse(
                    request.getRequestURI(),
                    request.getContextPath()
            ).pathWithinApplication();
            StringBuilder canonicalPath = new StringBuilder();
            for (PathContainer.Element element : path.elements()) {
                if (element instanceof PathContainer.PathSegment segment) {
                    canonicalPath.append(segment.valueToMatch());
                } else {
                    canonicalPath.append(element.value());
                }
            }
            return canonicalPath.toString();
        } catch (IllegalArgumentException failure) {
            throw new InvalidRequestPathException();
        }
    }

    static final class InvalidRequestPathException extends RuntimeException {
        private InvalidRequestPathException() {
            super("Request path is invalid");
        }
    }
}
