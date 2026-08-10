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

/**
 * Reports one completed role grant or revocation.
 *
 * @author RollW
 */
public final class UserRolesChangedEvent implements Event {
    /**
     * The completed role mutation.
     */
    public enum ChangeType {
        GRANTED,
        REVOKED
    }

    private final long userId;
    private final Role role;
    private final ChangeType changeType;
    private final Instant occurredAt;

    /**
     * Creates a role mutation event.
     *
     * @param userId     the affected user identifier
     * @param role       the role that changed
     * @param changeType the completed role mutation
     * @param occurredAt the mutation completion time
     */
    public UserRolesChangedEvent(long userId, Role role, ChangeType changeType, Instant occurredAt) {
        UserLifecycleEventSupport.requireUserId(userId);
        this.userId = userId;
        this.role = Objects.requireNonNull(role, "role");
        this.changeType = Objects.requireNonNull(changeType, "changeType");
        this.occurredAt = UserLifecycleEventSupport.requireInstant(occurredAt);
    }

    /**
     * Returns the affected user identifier.
     *
     * @return the user identifier
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Returns the changed role.
     *
     * @return the role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Returns the completed role mutation.
     *
     * @return the change type
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * Returns the mutation completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
