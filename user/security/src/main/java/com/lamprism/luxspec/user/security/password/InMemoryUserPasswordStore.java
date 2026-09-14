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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores encoded user passwords in a concurrent in-memory map.
 *
 * <p>This implementation is deliberately explicit about its local-only scope. It never accepts
 * raw passwords and uses compare-and-set replacement so concurrent password changes cannot
 * overwrite a newer representation.</p>
 *
 * @author RollW
 */
public class InMemoryUserPasswordStore implements UserPasswordStore {
    private final ConcurrentMap<Long, EncodedPassword> passwords = new ConcurrentHashMap<>();

    /**
     * Creates an empty password store.
     */
    public InMemoryUserPasswordStore() {
    }

    @Override
    public Optional<EncodedPassword> find(long userId) {
        requireUserId(userId);
        return Optional.ofNullable(passwords.get(userId));
    }

    @Override
    public boolean replace(
            long userId,
            EncodedPassword currentPassword,
            EncodedPassword replacementPassword
    ) {
        requireUserId(userId);
        EncodedPassword current = Objects.requireNonNull(currentPassword, "currentPassword");
        EncodedPassword replacement = Objects.requireNonNull(replacementPassword, "replacementPassword");
        return passwords.replace(userId, current, replacement);
    }

    /**
     * Installs or replaces a protected password representation.
     *
     * @param userId          the positive user ID
     * @param encodedPassword the protected representation
     */
    public void put(long userId, EncodedPassword encodedPassword) {
        requireUserId(userId);
        passwords.put(userId, Objects.requireNonNull(encodedPassword, "encodedPassword"));
    }

    /**
     * Removes a configured password representation.
     *
     * @param userId the positive user ID
     * @return whether a representation was removed
     */
    public boolean remove(long userId) {
        requireUserId(userId);
        return passwords.remove(userId) != null;
    }

    /**
     * Returns the number of configured passwords.
     *
     * @return the configured password count
     */
    public int size() {
        return passwords.size();
    }

    private static void requireUserId(long userId) {
        if (userId < 1L) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
