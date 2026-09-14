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
import com.lamprism.luxspec.security.token.SessionLifetime;

/**
 * Creates an application-specific Refresh Token Session projection.
 *
 * @param <S> the Refresh Token Session type
 * @author RollW
 */
@FunctionalInterface
public interface RefreshTokenSessionFactory<S extends RefreshTokenSession> {
    /**
     * Creates a Refresh Token Session for one authenticated actor.
     *
     * @param id             the generated stable session identifier
     * @param authentication the authentication that opens the session
     * @param lifetime       the configured session lifetime
     * @return the application session projection
     */
    S create(
            RefreshTokenSessionId id,
            Authentication authentication,
            SessionLifetime lifetime
    );
}
