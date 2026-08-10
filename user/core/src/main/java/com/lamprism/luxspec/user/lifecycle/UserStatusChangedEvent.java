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
import com.lamprism.luxspec.user.UserStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Reports that a user's lifecycle status changed.
 *
 * @author RollW
 */
public final class UserStatusChangedEvent implements Event {
    private final long userId;
    private final UserStatus status;
    private final Instant occurredAt;

    /**
     * Creates a status mutation event.
     *
     * @param userId     the affected user identifier
     * @param status     the resulting lifecycle status
     * @param occurredAt the mutation completion time
     */
    public UserStatusChangedEvent(long userId, UserStatus status, Instant occurredAt) {
        UserLifecycleEventSupport.requireUserId(userId);
        this.userId = userId;
        this.status = Objects.requireNonNull(status, "status");
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
     * Returns the resulting lifecycle status.
     *
     * @return the status
     */
    public UserStatus getStatus() {
        return status;
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
