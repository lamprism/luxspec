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

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Reports one atomic replacement of a user's complete role set.
 *
 * @author RollW
 */
public final class UserRolesReplacedEvent implements Event {
    private final long userId;
    private final Set<Role> roles;
    private final Instant occurredAt;

    /**
     * Creates a role replacement event.
     *
     * @param userId     the affected user identifier
     * @param roles      the replacement roles
     * @param occurredAt the mutation completion time
     */
    public UserRolesReplacedEvent(long userId, Set<Role> roles, Instant occurredAt) {
        UserLifecycleEventSupport.requireUserId(userId);
        this.userId = userId;
        this.roles = Set.copyOf(roles);
        if (this.roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        this.occurredAt = UserLifecycleEventSupport.requireInstant(occurredAt);
    }

    /**
     * Returns the affected user identifier.
     *
     * @return the user identifier
     */
    public long userId() {
        return userId;
    }

    /**
     * Returns the replacement roles.
     *
     * @return the immutable roles
     */
    public Set<Role> roles() {
        return roles;
    }

    /**
     * Returns the mutation completion time.
     *
     * @return the completion time
     */
    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UserRolesReplacedEvent event
                && userId == event.userId && roles.equals(event.roles) && occurredAt.equals(event.occurredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roles, occurredAt);
    }
}
