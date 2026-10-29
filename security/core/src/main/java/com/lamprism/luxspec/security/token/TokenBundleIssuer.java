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

package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.security.authentication.Authentication;

/**
 * Issues a collection of tokens as one application flow.
 *
 * @author RollW
 */
@FunctionalInterface
public interface TokenBundleIssuer {
    /**
     * Issues the tokens selected for this flow.
     *
     * @param authentication the authenticated actor
     * @param attributes     the explicit issuance attributes
     * @return the token issuance
     */
    TokenIssuance issue(Authentication authentication, ExecutionContext attributes);

    /**
     * Issues the selected tokens without additional attributes.
     *
     * @param authentication the authenticated actor
     * @return the token issuance
     */
    default TokenIssuance issue(Authentication authentication) {
        return issue(authentication, ExecutionContext.empty());
    }
}
