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

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the trusted client address for a Servlet request.
 *
 * <p>The default resolver uses {@link HttpServletRequest#getRemoteAddr()}. Implementations that
 * interpret forwarding headers must apply the application's trusted-proxy policy before returning
 * an address.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface ServletClientAddressResolver {
    /**
     * Resolves one trusted client address.
     *
     * @param request the current Servlet request
     * @return the normalized client address
     */
    String resolve(HttpServletRequest request);

    /**
     * Creates the default Servlet remote-address resolver.
     *
     * @return the default client-address resolver
     */
    static ServletClientAddressResolver remoteAddress() {
        return HttpServletRequest::getRemoteAddr;
    }
}
