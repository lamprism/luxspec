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

package com.lamprism.luxspec.user.resource;

import com.lamprism.luxspec.resource.ResourceException;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryUserStoreTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void combinesRegistrationProviderAndBrowsingRoles() {
        InMemoryUserStore store = new InMemoryUserStore(CLOCK);

        var user = store.register("alice", "alice@example.com", Set.of(Role.USER));

        assertEquals(user, store.provideByUsername("alice"));
        assertEquals(user, store.provide(user.getReference()));
        assertEquals(1, store.size());
        assertEquals(UserStatus.ACTIVE, user.status());
    }

    @Test
    void preservesIdentityIndexesAcrossLifecycleChanges() {
        InMemoryUserStore store = new InMemoryUserStore(CLOCK);
        var user = store.register("alice", null, Set.of(Role.USER));

        store.rename(user.id(), "bob");
        store.grantRole(user.id(), Role.ADMIN);
        store.disable(user.id());

        assertThrows(ResourceException.class, () -> store.provideByUsername("alice"));
        assertEquals("bob", store.provideByUsername("bob").username());
        assertEquals(Set.of(Role.USER, Role.ADMIN), store.find(user.id()).orElseThrow().roles());
        assertEquals(UserStatus.DISABLED, store.find(user.id()).orElseThrow().status());
    }

    @Test
    void rejectsEmptyInitialRoles() {
        InMemoryUserStore store = new InMemoryUserStore(CLOCK);

        assertThrows(
                IllegalArgumentException.class,
                () -> store.register("alice", null, Set.of())
        );
    }
}
