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

package com.lamprism.luxspec.user.security.password;

/**
 * Creates, verifies, and evaluates protected password representations.
 *
 * @author RollW
 */
public interface PasswordScheme {
    /**
     * Encodes one raw password for protected storage.
     *
     * @param rawPassword the raw password used only for this synchronous operation
     * @return the non-null opaque encoded password
     */
    EncodedPassword encode(CharSequence rawPassword);

    /**
     * Verifies one raw password against an encoded representation.
     *
     * @param rawPassword     the raw password used only for this synchronous operation
     * @param encodedPassword the protected stored representation
     * @return whether the password matches
     * @throws PasswordSchemeException when the stored representation cannot be processed safely
     */
    boolean verify(CharSequence rawPassword, EncodedPassword encodedPassword);

    /**
     * Reports whether one stored representation should be replaced after successful verification.
     *
     * @param encodedPassword the protected stored representation
     * @return whether the representation is below the current protection parameters
     * @throws PasswordSchemeException when the stored representation cannot be processed safely
     */
    boolean needsUpgrade(EncodedPassword encodedPassword);
}
