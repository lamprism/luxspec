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

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.security.authentication.Authentication;

import java.util.Objects;

/**
 * Derives an explicit execution context containing one successful Luxspec Authentication.
 *
 * <p>The bridge does not install or own context storage. The caller decides whether to pass the
 * derived value directly or expose it through an explicitly selected boundary.</p>
 *
 * @author RollW
 */
public final class SpringAuthenticationExecutionContextBridge {
    /**
     * Returns a context containing the supplied authentication.
     *
     * @param context        the current explicit context
     * @param authentication the successful Luxspec authentication
     * @return the derived context
     */
    public ExecutionContext withAuthentication(
            ExecutionContext context,
            Authentication authentication
    ) {
        ExecutionContext nonNullContext = Objects.requireNonNull(context, "context");
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        if (nonNullContext.get(Authentication.CONTEXT_KEY).isPresent()) {
            return nonNullContext.replace(Authentication.CONTEXT_KEY, nonNullAuthentication);
        }
        return nonNullContext.with(Authentication.CONTEXT_KEY, nonNullAuthentication);
    }
}
