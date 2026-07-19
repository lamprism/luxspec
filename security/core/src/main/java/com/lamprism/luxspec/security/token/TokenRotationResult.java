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
     * @param <S> the state type
     * @return the successful result
     */
    static <S> Succeeded<S> succeeded(S state) {
        return new Succeeded<>(state);
    }

    /**
     * Creates a rejected rotation result.
     *
     * @param reason the authoritative rejection reason
     * @param <S> the state type
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
