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
import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.SecurityContextKeys;

import java.util.Objects;

/**
 * Opens a nested ExecutionContext containing one successful Luxspec Authentication.
 *
 * @author RollW
 */
public class SpringAuthenticationExecutionContextBridge {
    /**
     * Opens a derived context that exposes the authenticated actor for downstream work.
     *
     * @param authentication the successful Luxspec authentication
     * @return the scope that restores the prior execution context
     */
    public ExecutionContexts.Scope open(Authentication authentication) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ExecutionContext currentContext = ExecutionContexts.current().orElse(ExecutionContext.empty());
        ExecutionContext authenticatedContext = currentContext.get(SecurityContextKeys.AUTHENTICATION).isPresent()
                ? currentContext.replace(SecurityContextKeys.AUTHENTICATION, nonNullAuthentication)
                : currentContext.with(SecurityContextKeys.AUTHENTICATION, nonNullAuthentication);
        return ExecutionContexts.open(authenticatedContext);
    }
}
