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

import java.time.Instant;

/**
 * Applies an application-selected response after a refresh-token replay is detected.
 *
 * <p>The store's atomic replay decision has already completed. Responses involving other stores
 * require their own transaction or consistency policy.</p>
 *
 * @param <S> the authoritative session type
 * @author RollW
 */
@FunctionalInterface
public interface RefreshTokenReplayHandler<S extends RefreshTokenSession> {
    /**
     * Leaves any further response to the store's configured replay policy.
     *
     * @param <S> the session type
     * @return the no-op response
     */
    static <S extends RefreshTokenSession> RefreshTokenReplayHandler<S> none() {
        return (session, detectedAt) -> {
        };
    }

    /**
     * Responds to a replay of a known consumed refresh token.
     *
     * @param session    the authoritative session associated with the token
     * @param detectedAt the detection time
     */
    void onReplay(S session, Instant detectedAt);
}
