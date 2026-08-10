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

import java.time.Instant;

/**
 * Reports that a user's login name was changed.
 *
 * <p>The new name is intentionally excluded from the event so event consumers cannot accidentally
 * copy identity data into general-purpose telemetry or audit fields.</p>
 *
 * @author RollW
 */
public final class UserRenamedEvent implements Event {
    private final long userId;
    private final Instant occurredAt;

    /**
     * Creates a user rename event.
     *
     * @param userId     the affected user identifier
     * @param occurredAt the mutation completion time
     */
    public UserRenamedEvent(long userId, Instant occurredAt) {
        UserLifecycleEventSupport.requireUserId(userId);
        this.userId = userId;
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
     * Returns the mutation completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
