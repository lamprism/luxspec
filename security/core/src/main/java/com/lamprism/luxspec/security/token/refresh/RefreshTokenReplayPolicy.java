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

/**
 * Selects the session mutation performed atomically when a consumed token is replayed.
 *
 * <p>Implementations must be thread-safe, nonblocking, and free of external side effects. A
 * separate response policy can act on the session returned in the replay rejection.</p>
 *
 * @param <S> the application session type
 * @author RollW
 */
@FunctionalInterface
public interface RefreshTokenReplayPolicy<S extends RefreshTokenSession> {
    /**
     * Selects revocation of the entire refresh session.
     *
     * @param <S> the session type
     * @return the revocation policy
     */
    static <S extends RefreshTokenSession> RefreshTokenReplayPolicy<S> revokeSession() {
        return session -> true;
    }

    /**
     * Rejects the presented token without revoking its current successor.
     *
     * @param <S> the session type
     * @return the reject-only policy
     */
    static <S extends RefreshTokenSession> RefreshTokenReplayPolicy<S> rejectOnly() {
        return session -> false;
    }

    /**
     * Determines whether replay revokes the session in the current store transaction.
     *
     * @param session the authoritative session associated with the consumed token
     * @return whether to revoke the session
     */
    boolean revokeSessionOnReplay(S session);
}
