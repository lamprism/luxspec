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

import com.lamprism.luxspec.event.Event;

import java.time.Instant;
import java.util.Objects;

/**
 * Reports that a user's protected password representation was replaced.
 *
 * <p>The raw password and both encoded representations are deliberately excluded.</p>
 *
 * @author RollW
 */
public final class UserPasswordChangedEvent implements Event {
    private final long userId;
    private final Instant occurredAt;

    /**
     * Creates a password replacement event.
     *
     * @param userId     the affected positive user identifier
     * @param occurredAt the replacement completion time
     */
    public UserPasswordChangedEvent(long userId, Instant occurredAt) {
        if (userId < 1L) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.userId = userId;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
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
     * Returns the replacement completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
