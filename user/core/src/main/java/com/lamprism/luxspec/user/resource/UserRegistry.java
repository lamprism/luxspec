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

package com.lamprism.luxspec.user.resource;

import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Owns generic account registration and lifecycle mutations.
 *
 * @author RollW
 */
public interface UserRegistry {
    /**
     * Registers a user with a non-empty initial role set.
     *
     * @param username the unique non-blank account name
     * @param email    the optional account email address
     * @param roles    the non-empty initial roles
     * @return the registered user
     */
    User register(String username, @Nullable String email, Set<Role> roles);

    /**
     * Changes a user's unique account name.
     *
     * @param userId   the user identifier
     * @param username the new non-blank unique account name
     */
    void rename(long userId, String username);

    /**
     * Changes a user's optional email address.
     *
     * @param userId the user identifier
     * @param email  the email address, or {@code null} to remove it
     */
    void changeEmail(long userId, @Nullable String email);

    /**
     * Adds one role to a user.
     *
     * @param userId the user identifier
     * @param role   the role to assign
     */
    void grantRole(long userId, Role role);

    /**
     * Removes one role when another role remains assigned.
     *
     * @param userId the user identifier
     * @param role   the role to remove
     */
    void revokeRole(long userId, Role role);

    /**
     * Changes the user lifecycle state to active.
     *
     * @param userId the user identifier
     */
    void activate(long userId);

    /**
     * Changes the user lifecycle state to disabled.
     *
     * @param userId the user identifier
     */
    void disable(long userId);

    /**
     * Changes the user lifecycle state to locked.
     *
     * @param userId the user identifier
     */
    void lock(long userId);

    /**
     * Changes the user lifecycle state to canceled.
     *
     * @param userId the user identifier
     */
    void cancel(long userId);
}
