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
 * Issues one kind of token for an already authenticated actor.
 *
 * @param <T> the token value type
 *
 * @author RollW
 */
@FunctionalInterface
public interface TokenIssuer<T extends Token> {
    /**
     * Issues one token for the current authentication flow.
     *
     * @param authentication the authenticated actor
     * @param attributes the explicit issuance attributes
     * @return the issued token
     */
    IssuedToken<T> issue(Authentication authentication, ExecutionContext attributes);

    /**
     * Issues a token without additional attributes.
     *
     * @param authentication the authenticated actor
     * @return the issued token
     */
    default IssuedToken<T> issue(Authentication authentication) {
        return issue(authentication, ExecutionContext.empty());
    }
}
