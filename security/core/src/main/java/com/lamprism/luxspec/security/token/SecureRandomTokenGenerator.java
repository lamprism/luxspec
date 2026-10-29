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

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Function;

/**
 * Generates URL-safe opaque tokens from cryptographically secure random bytes.
 *
 * @param <T> the token value type
 * @author RollW
 */
public final class SecureRandomTokenGenerator<T extends Token> implements TokenGenerator<T> {
    private final SecureRandom random;
    private final int byteLength;
    private final Function<String, T> tokenFactory;

    /**
     * Creates a secure generator with an application-selected token value factory.
     *
     * @param random       the cryptographically secure random source
     * @param byteLength   the number of random bytes, at least 16
     * @param tokenFactory the token kind's value factory
     */
    public SecureRandomTokenGenerator(SecureRandom random, int byteLength, Function<String, T> tokenFactory) {
        this.random = Objects.requireNonNull(random, "random");
        if (byteLength < 16) {
            throw new IllegalArgumentException("byteLength must be at least 16");
        }
        this.byteLength = byteLength;
        this.tokenFactory = Objects.requireNonNull(tokenFactory, "tokenFactory");
    }

    @Override
    public T generate() {
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return Objects.requireNonNull(tokenFactory.apply(value), "generated token");
    }
}
