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

import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.SessionLifetime;
import com.lamprism.luxspec.security.token.TokenSession;

/**
 * Represents the server-authoritative renewable authorization behind rotated Refresh Tokens.
 *
 * @author RollW
 */
public interface RefreshTokenSession extends TokenSession<RefreshTokenSessionId> {
    /**
     * Creates a validated immutable Refresh Token Session.
     *
     * @param id                   the stable session identifier
     * @param subject              the session subject
     * @param authorizationCeiling the maximum grants retained across refreshes
     * @param lifetime             the idle and absolute lifetime bounds
     * @return the immutable Refresh Token Session
     */
    static RefreshTokenSession of(
            RefreshTokenSessionId id,
            Subject subject,
            AuthorizationGrantSet authorizationCeiling,
            SessionLifetime lifetime
    ) {
        return new ImmutableRefreshTokenSession(id, subject, authorizationCeiling, lifetime);
    }

    /**
     * Returns the maximum authorization retained across refreshes.
     *
     * @return the immutable authorization ceiling
     */
    AuthorizationGrantSet getAuthorizationCeiling();
}
