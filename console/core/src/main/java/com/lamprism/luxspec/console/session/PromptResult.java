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

package com.lamprism.luxspec.console.session;

import java.util.Objects;

/**
 * Explicit result of a command session prompt.
 *
 * @author RollW
 */
public sealed interface PromptResult<T>
        permits PromptResult.Accepted, PromptResult.Cancelled,
        PromptResult.Unavailable, PromptResult.EndOfInput {
    /**
     * Prompt completion states.
     */
    enum Status {
        /**
         * A value was accepted.
         */
        ACCEPTED,
        /**
         * The user canceled the prompt.
         */
        CANCELLED,
        /**
         * The current provider cannot prompt.
         */
        UNAVAILABLE,
        /**
         * The input source ended before a value was accepted.
         */
        END_OF_INPUT
    }

    /**
     * @return the explicit completion state
     */
    Status getStatus();

    /**
     * @return whether a value was accepted
     */
    default boolean isAccepted() {
        return getStatus() == Status.ACCEPTED;
    }

    /**
     * Creates an accepted result.
     *
     * @param value accepted prompt value
     * @param <T>   prompt value type
     * @return an accepted prompt result
     */
    static <T> PromptResult<T> accepted(T value) {
        return new Accepted<>(value);
    }

    /**
     * Creates a canceled result.
     *
     * @param <T> prompt value type
     * @return a canceled prompt result
     */
    static <T> PromptResult<T> cancelled() {
        return new Cancelled<>();
    }

    /**
     * Creates an unavailable interaction result.
     *
     * @param <T> prompt value type
     * @return an unavailable prompt result
     */
    static <T> PromptResult<T> unavailable() {
        return new Unavailable<>();
    }

    /**
     * Creates an end-of-input result.
     *
     * @param <T> prompt value type
     * @return an end-of-input prompt result
     */
    static <T> PromptResult<T> endOfInput() {
        return new EndOfInput<>();
    }

    /**
     * Accepted typed prompt value.
     */
    final class Accepted<T> implements PromptResult<T> {
        private final T value;

        private Accepted(T value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        /**
         * @return the accepted prompt value
         */
        public T getValue() {
            return value;
        }

        @Override
        public Status getStatus() {
            return Status.ACCEPTED;
        }
    }

    /**
     * User canceled the prompt.
     */
    final class Cancelled<T> implements PromptResult<T> {
        private Cancelled() {
        }

        @Override
        public Status getStatus() {
            return Status.CANCELLED;
        }
    }

    /**
     * The current session cannot provide interactive input.
     */
    final class Unavailable<T> implements PromptResult<T> {
        private Unavailable() {
        }

        @Override
        public Status getStatus() {
            return Status.UNAVAILABLE;
        }
    }

    /**
     * The session reached end of input before accepting a value.
     */
    final class EndOfInput<T> implements PromptResult<T> {
        private EndOfInput() {
        }

        @Override
        public Status getStatus() {
            return Status.END_OF_INPUT;
        }
    }
}
