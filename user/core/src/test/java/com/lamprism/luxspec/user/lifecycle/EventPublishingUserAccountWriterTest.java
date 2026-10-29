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
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.resource.UserAccountWriter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class EventPublishingUserAccountWriterTest {
    private static final Instant NOW = Instant.parse("2026-08-09T00:00:00Z");

    @Test
    void publishesOneFactAfterEachCompletedMutation() {
        List<Event> events = new ArrayList<>();
        EventPublishingUserAccountWriter writer = new EventPublishingUserAccountWriter(
                new TestUserAccountWriter(),
                events::add,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        writer.register("alice", null, Set.of(Role.USER));
        writer.rename(7L, "alice-2");
        writer.changeEmail(7L, "alice@example.com");
        writer.grantRole(7L, Role.ADMIN);
        writer.revokeRole(7L, Role.ADMIN);
        writer.changeStatus(7L, UserStatus.ACTIVE);
        writer.changeStatus(7L, UserStatus.DISABLED);
        writer.changeStatus(7L, UserStatus.LOCKED);
        writer.changeStatus(7L, UserStatus.CANCELED);

        assertEquals(
                List.of(
                        UserRegisteredEvent.class,
                        UserRenamedEvent.class,
                        UserEmailChangedEvent.class,
                        UserRolesChangedEvent.class,
                        UserRolesChangedEvent.class,
                        UserStatusChangedEvent.class,
                        UserStatusChangedEvent.class,
                        UserStatusChangedEvent.class,
                        UserStatusChangedEvent.class
                ),
                events.stream().map(Object::getClass).toList()
        );
        UserRolesChangedEvent roleEvent = (UserRolesChangedEvent) events.get(3);
        assertEquals(UserRolesChangedEvent.ChangeType.GRANTED, roleEvent.getChangeType());
        UserStatusChangedEvent statusEvent = (UserStatusChangedEvent) events.get(8);
        assertEquals(UserStatus.CANCELED, statusEvent.getStatus());
    }

    @Test
    void publishesOneEventForAnAtomicRoleReplacement() {
        List<Event> events = new ArrayList<>();
        EventPublishingUserAccountWriter writer = new EventPublishingUserAccountWriter(
                new TestUserAccountWriter(), events::add, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        writer.replaceRoles(7L, Set.of(Role.ADMIN));

        assertEquals(List.of(new UserRolesReplacedEvent(7L, Set.of(Role.ADMIN), NOW)), events);
    }

    @Test
    void doesNotPublishWhenTheDelegatedMutationFails() {
        List<Event> events = new ArrayList<>();
        TestUserAccountWriter delegate = new TestUserAccountWriter();
        delegate.fail = true;
        EventPublishingUserAccountWriter writer = new EventPublishingUserAccountWriter(delegate, events::add);

        assertThrows(IllegalStateException.class, () -> writer.changeStatus(7L, UserStatus.DISABLED));

        assertEquals(List.of(), events);
    }

    private static User user() {
        return new User(
                7L,
                "alice",
                null,
                Set.of(Role.USER),
                UserStatus.ACTIVE,
                NOW,
                NOW
        );
    }

    private static final class TestUserAccountWriter implements UserAccountWriter {
        private boolean fail;

        @Override
        public User register(String username, @Nullable String email, Set<Role> roles, UserStatus status) {
            check();
            return user();
        }

        @Override
        public void rename(long userId, String username) {
            check();
        }

        @Override
        public void changeEmail(long userId, @Nullable String email) {
            check();
        }

        @Override
        public void grantRole(long userId, Role role) {
            check();
        }

        @Override
        public void revokeRole(long userId, Role role) {
            check();
        }

        @Override
        public void replaceRoles(long userId, Set<Role> roles) {
            check();
        }

        @Override
        public void changeStatus(long userId, UserStatus status) {
            check();
        }

        private void check() {
            if (fail) {
                throw new IllegalStateException("mutation failed");
            }
        }
    }
}
