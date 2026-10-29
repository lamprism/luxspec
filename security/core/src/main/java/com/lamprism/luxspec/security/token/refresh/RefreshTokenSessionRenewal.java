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

import com.lamprism.luxspec.security.token.SessionLifetime;

/**
 * Rebuilds a refresh session with a renewed lifetime while preserving application metadata.
 *
 * @param <S> the application session type
 * @author RollW
 */
@FunctionalInterface
public interface RefreshTokenSessionRenewal<S extends RefreshTokenSession> {
    /**
     * Rebuilds the standard refresh session.
     *
     * @return the standard session renewal
     */
    static RefreshTokenSessionRenewal<RefreshTokenSession> standard() {
        return (session, lifetime) -> RefreshTokenSession.of(
                session.getId(), session.getSubject(), session.getAuthorizationCeiling(), lifetime
        );
    }

    /**
     * Returns the session with its renewed lifetime and otherwise unchanged identity and grants.
     *
     * @param session  the current session
     * @param lifetime the renewed lifetime
     * @return the renewed session
     */
    S renew(S session, SessionLifetime lifetime);
}
