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
 * Reads and atomically mutates protected password representations for user accounts.
 *
 * <p>The caller owns authorization and any transaction that must combine these operations with
 * account, security-version, or event state.</p>
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
     * Initializes a password only when the user has no existing password representation.
     *
     * @param userId   the positive user identifier
     * @param password the encoded password
     * @return whether the password was initialized
     */
    boolean initialize(long userId, EncodedPassword password);

    /**
     * Replaces a password only when the verified current representation still matches.
     *
     * @param userId              the positive user identifier
     * @param currentPassword     the representation that was verified
     * @param replacementPassword the newly encoded representation
     * @param purpose whether the operation changes the credential or only upgrades its encoding
     * @return whether the replacement was applied
     */
    boolean replace(
            long userId,
            EncodedPassword currentPassword,
            EncodedPassword replacementPassword,
            PasswordReplacementPurpose purpose
    );

    /**
     * Replaces an existing password without comparing the previous representation.
     *
     * <p>Authorization and account-state checks are the caller's responsibility.</p>
     *
     * @param userId   the positive user identifier
     * @param password the new encoded password
     * @return whether an existing password was reset
     */
    boolean reset(long userId, EncodedPassword password);
}
