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

import java.time.Instant;

/**
 * Atomically consumes a token and installs its successor.
 *
 * @param <T> the token value type
 * @param <S> the associated authoritative state type
 * @author RollW
 */
@FunctionalInterface
public interface TokenRotator<T extends Token, S> {
    /**
     * Applies one rotation at the supplied time.
     *
     * @param presented the token to consume
     * @param successor the newly generated token
     * @param rotatedAt the rotation time
     * @return the updated authoritative state
     * @throws TokenRotationException when rotation is rejected
     */
    S rotate(T presented, T successor, Instant rotatedAt);
}
