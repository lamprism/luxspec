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
import com.lamprism.luxspec.user.resource.UserAccountWriter;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Publishes user lifecycle facts after a delegated account write completes.
 *
 * <p>User persistence remains application-owned. This decorator lets an application add the
 * provider-independent event contract without changing its
 * {@link UserAccountWriter} implementation or making that implementation depend on audit classes.
 * Events are published synchronously after the delegate returns. The publisher or caller must
 * coordinate delivery with an enclosing transaction when after-commit delivery is required.</p>
 *
 * @author RollW
 */
public class EventPublishingUserAccountWriter implements UserAccountWriter {
    private final UserAccountWriter delegate;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a writer decorator using the UTC system clock.
     *
     * @param delegate       the application-owned user account writer
     * @param eventPublisher the lifecycle event publisher
     */
    public EventPublishingUserAccountWriter(UserAccountWriter delegate, EventPublisher eventPublisher) {
        this(delegate, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates a writer decorator with an explicit event clock.
     *
     * @param delegate       the application-owned user account writer
     * @param eventPublisher the lifecycle event publisher
     * @param clock          the event timestamp clock
     */
    public EventPublishingUserAccountWriter(UserAccountWriter delegate, EventPublisher eventPublisher, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public User register(String username, @Nullable String email, Set<Role> roles, UserStatus status) {
        User user = delegate.register(username, email, roles, status);
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
    public void replaceRoles(long userId, Set<Role> roles) {
        Set<Role> replacement = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        delegate.replaceRoles(userId, replacement);
        eventPublisher.publish(new UserRolesReplacedEvent(userId, replacement, clock.instant()));
    }

    @Override
    public void changeStatus(long userId, UserStatus status) {
        UserStatus nonNullStatus = Objects.requireNonNull(status, "status");
        delegate.changeStatus(userId, nonNullStatus);
        Instant occurredAt = clock.instant();
        eventPublisher.publish(new UserStatusChangedEvent(userId, nonNullStatus, occurredAt));
    }
}
