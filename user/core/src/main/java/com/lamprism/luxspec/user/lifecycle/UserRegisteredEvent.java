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

package com.lamprism.luxspec.user.lifecycle;

import com.lamprism.luxspec.event.Event;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Reports that a user account was registered.
 *
 * <p>The event contains the stable identifier and authorization state only. It does not copy the
 * username, email address, password, or persistence entity.</p>
 *
 * @author RollW
 */
public final class UserRegisteredEvent implements Event {
    private final long userId;
    private final Set<Role> roles;
    private final UserStatus status;
    private final Instant occurredAt;

    /**
     * Creates a registration event from the completed user snapshot.
     *
     * @param user       the completed immutable user snapshot
     * @param occurredAt the registration completion time
     */
    public UserRegisteredEvent(User user, Instant occurredAt) {
        User nonNullUser = Objects.requireNonNull(user, "user");
        this.userId = nonNullUser.id();
        this.roles = Set.copyOf(nonNullUser.roles());
        this.status = nonNullUser.status();
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    /**
     * Returns the registered user identifier.
     *
     * @return the user identifier
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Returns the immutable initial role set.
     *
     * @return the initial roles
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * Returns the initial lifecycle status.
     *
     * @return the status
     */
    public UserStatus getStatus() {
        return status;
    }

    /**
     * Returns the registration completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
