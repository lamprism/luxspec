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

/**
 * Produces a typed one-way digest of a sensitive token value.
 *
 * @param <T> the token value type
 * @author RollW
 */
@FunctionalInterface
public interface TokenHasher<T extends Token> {
    /**
     * Produces a one-way Digest for a sensitive Token.
     *
     * @param token the Token to hash
     * @return the typed Token Digest
     */
    TokenDigest<T> hash(T token);
}
