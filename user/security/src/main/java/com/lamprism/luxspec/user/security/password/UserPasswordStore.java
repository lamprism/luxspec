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

import java.util.Optional;

/**
 * Reads and conditionally replaces protected password representations for user accounts.
 *
 * @author RollW
 */
public interface UserPasswordStore {
    /**
     * Finds the configured password for one user.
     *
     * @param userId the positive user identifier
     * @return the protected password, or empty when password authentication is not configured
     */
    Optional<EncodedPassword> find(long userId);

    /**
     * Replaces a password only when the verified current representation still matches.
     *
     * @param userId              the positive user identifier
     * @param currentPassword     the representation that was verified
     * @param replacementPassword the newly encoded representation
     * @return whether the replacement was applied
     */
    boolean replace(long userId, EncodedPassword currentPassword, EncodedPassword replacementPassword);
}
