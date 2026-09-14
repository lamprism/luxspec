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
 * Verifies a token and returns a trusted provider-neutral result.
 *
 * @param <T> the token value type
 * @param <V> the verification result type
 * @author RollW
 */
@FunctionalInterface
public interface TokenVerifier<T extends Token, V extends TokenVerification<T>> {
    /**
     * Verifies a Token and returns trusted provider-neutral data.
     *
     * @param token the untrusted presented Token
     * @return the trusted verification result
     */
    V verify(T token);
}
