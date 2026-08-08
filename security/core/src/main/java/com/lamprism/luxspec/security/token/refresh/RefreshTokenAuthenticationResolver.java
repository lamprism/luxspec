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

package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;

/**
 * Reconstructs current Authentication for one successfully rotated Refresh Token Session.
 *
 * @author RollW
 */
@FunctionalInterface
public interface RefreshTokenAuthenticationResolver<S extends RefreshTokenSession> {
    /**
     * Reconstructs the current authenticated actor and applies refresh-flow state policy.
     *
     * @param session the authoritative Refresh Token Session
     * @return the current authentication used to issue successor tokens
     * @throws AuthenticationException when the subject may no longer authenticate
     */
    Authentication resolve(S session);
}
