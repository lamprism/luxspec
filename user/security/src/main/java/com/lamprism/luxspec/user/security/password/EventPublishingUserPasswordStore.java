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
 * <p>Only successful mutations are published. The decorator never reads or copies password
 * representation values into the event. Publication occurs synchronously after the delegate
 * returns; the caller or publisher owns any after-commit or transactional delivery policy.</p>
 *
 * @author RollW
 */
public class EventPublishingUserPasswordStore implements UserPasswordStore {
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
    public boolean initialize(long userId, EncodedPassword password) {
        boolean initialized = delegate.initialize(userId, password);
        if (initialized) {
            publish(userId, UserPasswordChangedEvent.ChangeType.INITIALIZED);
        }
        return initialized;
    }

    @Override
    public boolean replace(
            long userId,
            EncodedPassword currentPassword,
            EncodedPassword replacementPassword,
            PasswordReplacementPurpose purpose
    ) {
        PasswordReplacementPurpose nonNullPurpose = Objects.requireNonNull(purpose, "purpose");
        boolean replaced = delegate.replace(userId, currentPassword, replacementPassword, nonNullPurpose);
        if (!replaced) {
            return false;
        }
        UserPasswordChangedEvent.ChangeType type = nonNullPurpose == PasswordReplacementPurpose.REHASH
                ? UserPasswordChangedEvent.ChangeType.REHASHED
                : UserPasswordChangedEvent.ChangeType.CHANGED;
        publish(userId, type);
        return true;
    }

    @Override
    public boolean reset(long userId, EncodedPassword password) {
        boolean reset = delegate.reset(userId, password);
        if (reset) {
            publish(userId, UserPasswordChangedEvent.ChangeType.RESET);
        }
        return reset;
    }

    private void publish(long userId, UserPasswordChangedEvent.ChangeType type) {
        eventPublisher.publish(new UserPasswordChangedEvent(userId, type, clock.instant()));
    }
}
