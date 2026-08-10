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

import com.lamprism.luxspec.event.EventPublisher;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Adds safe password replacement events to an application-owned password store.
 *
 * <p>Only successful replacements are published. The decorator never reads or copies password
 * representation values into the event.</p>
 *
 * @author RollW
 */
public final class EventPublishingUserPasswordStore implements UserPasswordStore {
    private final UserPasswordStore delegate;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a password store decorator using the UTC system clock.
     *
     * @param delegate       the application-owned password store
     * @param eventPublisher the password lifecycle event publisher
     */
    public EventPublishingUserPasswordStore(
            UserPasswordStore delegate,
            EventPublisher eventPublisher
    ) {
        this(delegate, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates a password store decorator with an explicit event clock.
     *
     * @param delegate       the application-owned password store
     * @param eventPublisher the password lifecycle event publisher
     * @param clock          the event timestamp clock
     */
    public EventPublishingUserPasswordStore(
            UserPasswordStore delegate,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Optional<EncodedPassword> find(long userId) {
        return delegate.find(userId);
    }

    @Override
    public boolean replace(
            long userId,
            EncodedPassword currentPassword,
            EncodedPassword replacementPassword
    ) {
        boolean replaced = delegate.replace(userId, currentPassword, replacementPassword);
        if (!replaced) {
            return false;
        }
        eventPublisher.publish(new UserPasswordChangedEvent(userId, clock.instant()));
        return true;
    }
}
