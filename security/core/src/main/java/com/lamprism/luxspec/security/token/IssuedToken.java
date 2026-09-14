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
 * Describes one newly issued token and its exact lifetime.
 *
 * @param <T> the token value type
 * @author RollW
 */
public interface IssuedToken<T extends Token> {
    /**
     * Creates a validated immutable issued-token value.
     *
     * @param token     the newly issued token
     * @param issuedAt  the issuance time
     * @param expiresAt the exclusive expiration time
     * @param <T>       the token value type
     * @return the immutable issued token
     */
    static <T extends Token> IssuedToken<T> of(T token, Instant issuedAt, Instant expiresAt) {
        return new ImmutableIssuedToken<>(token, issuedAt, expiresAt);
    }

    /**
     * Returns the issued sensitive token value.
     *
     * @return the issued token
     */
    T getToken();

    /**
     * Returns the issuance time.
     *
     * @return the issuance time
     */
    Instant getIssuedAt();

    /**
     * Returns the exclusive expiration time.
     *
     * @return the expiration time
     */
    Instant getExpiresAt();
}
