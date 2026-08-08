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

import java.util.Objects;

/**
 * Describes the authoritative result of one atomic token rotation.
 *
 * @param <S> the updated server-authoritative state type
 * @author RollW
 */
public sealed interface TokenRotationResult<S>
        permits TokenRotationResult.Succeeded, TokenRotationResult.Rejected {
    /**
     * Creates a successful rotation result.
     *
     * @param state the updated server-authoritative state
     * @param <S>   the state type
     * @return the successful result
     */
    static <S> Succeeded<S> succeeded(S state) {
        return new Succeeded<>(state);
    }

    /**
     * Creates a rejected rotation result.
     *
     * @param reason the authoritative rejection reason
     * @param <S>    the state type
     * @return the rejected result
     */
    static <S> Rejected<S> rejected(TokenRotationRejection reason) {
        return new Rejected<>(reason);
    }

    /**
     * Contains the updated state after a successful rotation.
     *
     * @param <S> the state type
     */
    final class Succeeded<S> implements TokenRotationResult<S> {
        private final S state;

        private Succeeded(S state) {
            this.state = Objects.requireNonNull(state, "state");
        }

        /**
         * Returns the updated server-authoritative state.
         *
         * @return the updated state
         */
        public S getState() {
            return state;
        }
    }

    /**
     * Contains the authoritative reason for a rejected rotation.
     *
     * @param <S> the state type
     */
    final class Rejected<S> implements TokenRotationResult<S> {
        private final TokenRotationRejection reason;

        private Rejected(TokenRotationRejection reason) {
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        /**
         * Returns the authoritative rejection reason.
         *
         * @return the rejection reason
         */
        public TokenRotationRejection getReason() {
            return reason;
        }
    }
}
