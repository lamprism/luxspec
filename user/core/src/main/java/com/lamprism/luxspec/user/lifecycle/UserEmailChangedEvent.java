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
 * Reports that a user's optional email assignment changed.
 *
 * <p>The address itself is never retained. The presence flag is sufficient for lifecycle audit
 * and cache invalidation consumers.</p>
 *
 * @author RollW
 */
public final class UserEmailChangedEvent implements Event {
    private final long userId;
    private final boolean emailPresent;
    private final Instant occurredAt;

    /**
     * Creates an email mutation event.
     *
     * @param userId       the affected user identifier
     * @param emailPresent whether an email is assigned after the mutation
     * @param occurredAt   the mutation completion time
     */
    public UserEmailChangedEvent(long userId, boolean emailPresent, Instant occurredAt) {
        UserLifecycleEventSupport.requireUserId(userId);
        this.userId = userId;
        this.emailPresent = emailPresent;
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
     * Reports whether an email is assigned after the mutation.
     *
     * @return {@code true} when an email is assigned
     */
    public boolean isEmailPresent() {
        return emailPresent;
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
