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

package com.lamprism.luxspec.security.spring.authentication;

import com.lamprism.luxspec.security.token.access.AccessToken;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Resolves an access token from a Servlet request.
 *
 * @author RollW
 */
@FunctionalInterface
public interface BearerTokenResolver {
    /**
     * Resolves a token when one is present.
     *
     * @param request the current request
     * @return the resolved token, or empty if the request has none
     */
    Optional<AccessToken> resolve(HttpServletRequest request);
}
