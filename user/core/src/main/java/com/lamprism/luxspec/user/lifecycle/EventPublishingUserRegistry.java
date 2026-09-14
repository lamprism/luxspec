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

import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.resource.UserRegistry;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Publishes user lifecycle facts after a delegated registry mutation completes.
 *
 * <p>User persistence remains application-owned. This decorator lets an application add the
 * provider-independent event contract without changing its {@link UserRegistry} implementation or
 * making that implementation depend on audit classes.</p>
 *
 * @author RollW
 */
public class EventPublishingUserRegistry implements UserRegistry {
    private final UserRegistry delegate;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a registry decorator using the UTC system clock.
     *
     * @param delegate       the application-owned user registry
     * @param eventPublisher the lifecycle event publisher
     */
    public EventPublishingUserRegistry(UserRegistry delegate, EventPublisher eventPublisher) {
        this(delegate, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates a registry decorator with an explicit event clock.
     *
     * @param delegate       the application-owned user registry
     * @param eventPublisher the lifecycle event publisher
     * @param clock          the event timestamp clock
     */
    public EventPublishingUserRegistry(UserRegistry delegate, EventPublisher eventPublisher, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public User register(String username, @Nullable String email, Set<Role> roles) {
        User user = delegate.register(username, email, roles);
        eventPublisher.publish(new UserRegisteredEvent(user, clock.instant()));
        return user;
    }

    @Override
    public void rename(long userId, String username) {
        delegate.rename(userId, username);
        eventPublisher.publish(new UserRenamedEvent(userId, clock.instant()));
    }

    @Override
    public void changeEmail(long userId, @Nullable String email) {
        delegate.changeEmail(userId, email);
        eventPublisher.publish(new UserEmailChangedEvent(userId, email != null, clock.instant()));
    }

    @Override
    public void grantRole(long userId, Role role) {
        delegate.grantRole(userId, role);
        eventPublisher.publish(new UserRolesChangedEvent(
                userId,
                role,
                UserRolesChangedEvent.ChangeType.GRANTED,
                clock.instant()
        ));
    }

    @Override
    public void revokeRole(long userId, Role role) {
        delegate.revokeRole(userId, role);
        eventPublisher.publish(new UserRolesChangedEvent(
                userId,
                role,
                UserRolesChangedEvent.ChangeType.REVOKED,
                clock.instant()
        ));
    }

    @Override
    public void activate(long userId) {
        changeStatus(userId, UserStatus.ACTIVE, delegate::activate);
    }

    @Override
    public void disable(long userId) {
        changeStatus(userId, UserStatus.DISABLED, delegate::disable);
    }

    @Override
    public void lock(long userId) {
        changeStatus(userId, UserStatus.LOCKED, delegate::lock);
    }

    @Override
    public void cancel(long userId) {
        changeStatus(userId, UserStatus.CANCELED, delegate::cancel);
    }

    private void changeStatus(long userId, UserStatus status, UserStatusMutation mutation) {
        mutation.apply(userId);
        Instant occurredAt = clock.instant();
        eventPublisher.publish(new UserStatusChangedEvent(userId, status, occurredAt));
    }

    @FunctionalInterface
    private interface UserStatusMutation {
        void apply(long userId);
    }
}
