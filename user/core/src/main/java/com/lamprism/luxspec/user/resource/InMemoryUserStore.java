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

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.QueryComplexityLimits;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.resource.InMemoryResourceBrowser;
import com.lamprism.luxspec.resource.ResourceErrorCode;
import com.lamprism.luxspec.resource.ResourceException;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.query.UserBrowser;
import com.lamprism.luxspec.user.query.UserQueryFields;
import com.lamprism.luxspec.user.query.UserQuerySchema;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Provides a concurrent in-memory user registry, provider, and browser.
 *
 * <p>The store is intended for local applications, tests, and examples. It keeps immutable user
 * snapshots and performs all identity and username-index mutations under one lock, so a query
 * never observes a partially applied to rename or lifecycle update.</p>
 *
 * @author RollW
 */
public class InMemoryUserStore implements UserRegistry, UserProvider, UserBrowser {
    private final Object stateLock = new Object();
    private final Clock clock;
    private final Map<Long, User> usersById = new LinkedHashMap<>();
    private final Map<String, Long> idsByUsername = new HashMap<>();
    private final InMemoryResourceBrowser<Long, User> browser;
    private long nextId;

    /**
     * Creates an empty store with UTC system time and IDs starting at one.
     */
    public InMemoryUserStore() {
        this(Clock.systemUTC(), 1L);
    }

    /**
     * Creates an empty store with an explicit clock and IDs starting at one.
     *
     * @param clock the clock used for registration and mutation timestamps
     */
    public InMemoryUserStore(Clock clock) {
        this(clock, 1L);
    }

    /**
     * Creates an empty store with explicit clock and first-ID settings.
     *
     * @param clock       the clock used for registration and mutation timestamps
     * @param firstUserId the first positive ID to allocate
     */
    public InMemoryUserStore(Clock clock, long firstUserId) {
        this.clock = Objects.requireNonNull(clock, "clock");
        if (firstUserId < 1L) {
            throw new IllegalArgumentException("firstUserId must be positive");
        }
        this.nextId = firstUserId;
        this.browser = InMemoryResourceBrowser.builder(
                        UserResourceTypes.USER,
                        this::snapshot
                )
                .field(UserQueryFields.ID, User::id)
                .field(UserQueryFields.USERNAME, User::username)
                .field(UserQueryFields.EMAIL, User::email)
                .field(UserQueryFields.STATUS, User::status)
                .field(UserQueryFields.REGISTERED_AT, User::registeredAt)
                .field(UserQueryFields.UPDATED_AT, User::updatedAt)
                .build();
    }

    @Override
    public User register(String username, @Nullable String email, Set<Role> roles) {
        String nonBlankUsername = requireUsername(username);
        Set<Role> nonEmptyRoles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        if (nonEmptyRoles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        synchronized (stateLock) {
            if (idsByUsername.containsKey(nonBlankUsername)) {
                throw new IllegalArgumentException("Username is already registered: " + nonBlankUsername);
            }
            if (nextId < 1L) {
                throw new IllegalStateException("No user IDs remain");
            }
            long id = nextId++;
            Instant now = Objects.requireNonNull(clock.instant(), "clock instant");
            User user = new User(
                    id,
                    nonBlankUsername,
                    email,
                    nonEmptyRoles,
                    UserStatus.ACTIVE,
                    now,
                    now
            );
            usersById.put(id, user);
            idsByUsername.put(nonBlankUsername, id);
            return user;
        }
    }

    @Override
    public void rename(long userId, String username) {
        String nonBlankUsername = requireUsername(username);
        synchronized (stateLock) {
            User current = requireUser(userId);
            Long owner = idsByUsername.get(nonBlankUsername);
            if (owner != null && owner.longValue() != userId) {
                throw new IllegalArgumentException("Username is already registered: " + nonBlankUsername);
            }
            if (current.username().equals(nonBlankUsername)) {
                return;
            }
            replace(current, user -> copy(user, nonBlankUsername, user.email(), user.roles(), user.status()));
            idsByUsername.remove(current.username());
            idsByUsername.put(nonBlankUsername, userId);
        }
    }

    @Override
    public void changeEmail(long userId, @Nullable String email) {
        update(userId, user -> copy(user, user.username(), email, user.roles(), user.status()));
    }

    @Override
    public void grantRole(long userId, Role role) {
        Role nonNullRole = Objects.requireNonNull(role, "role");
        update(userId, user -> {
            if (user.roles().contains(nonNullRole)) {
                return user;
            }
            Set<Role> roles = new LinkedHashSet<>(user.roles());
            roles.add(nonNullRole);
            return copy(user, user.username(), user.email(), roles, user.status());
        });
    }

    @Override
    public void revokeRole(long userId, Role role) {
        Role nonNullRole = Objects.requireNonNull(role, "role");
        update(userId, user -> {
            if (!user.roles().contains(nonNullRole)) {
                return user;
            }
            if (user.roles().size() == 1) {
                throw new IllegalStateException("A user must retain at least one role");
            }
            Set<Role> roles = new LinkedHashSet<>(user.roles());
            roles.remove(nonNullRole);
            return copy(user, user.username(), user.email(), roles, user.status());
        });
    }

    @Override
    public void activate(long userId) {
        changeStatus(userId, UserStatus.ACTIVE);
    }

    @Override
    public void disable(long userId) {
        changeStatus(userId, UserStatus.DISABLED);
    }

    @Override
    public void lock(long userId) {
        changeStatus(userId, UserStatus.LOCKED);
    }

    @Override
    public void cancel(long userId) {
        changeStatus(userId, UserStatus.CANCELED);
    }

    @Override
    public ResourceType<Long> getResourceType() {
        return UserResourceTypes.USER;
    }

    @Override
    public User provide(ResourceReference<Long> reference) {
        ResourceReference<Long> nonNullReference = requireUserReference(reference);
        synchronized (stateLock) {
            return requireUser(nonNullReference.id());
        }
    }

    @Override
    public List<User> provide(Collection<ResourceReference<Long>> references) {
        List<ResourceReference<Long>> requested = List.copyOf(
                Objects.requireNonNull(references, "references")
        );
        synchronized (stateLock) {
            List<User> users = new ArrayList<>(requested.size());
            for (ResourceReference<Long> reference : requested) {
                users.add(requireUser(requireUserReference(reference).id()));
            }
            return List.copyOf(users);
        }
    }

    @Override
    public User provideByUsername(String username) {
        String nonBlankUsername = requireUsername(username);
        synchronized (stateLock) {
            Long userId = idsByUsername.get(nonBlankUsername);
            if (userId == null) {
                throw notFound();
            }
            return requireUser(userId);
        }
    }

    @Override
    public QueryResult<User> browse(QueryCriteria criteria, QueryWindow window) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        UserQuerySchema.standard().validate(nonNullCriteria, QueryComplexityLimits.defaults());
        return browser.browse(nonNullCriteria, Objects.requireNonNull(window, "window"));
    }

    /**
     * Returns a user when the ID exists without converting absence into an exception.
     *
     * @param userId the user ID
     * @return the user, or empty when no user exists
     */
    public Optional<User> find(long userId) {
        synchronized (stateLock) {
            return Optional.ofNullable(usersById.get(userId));
        }
    }

    /**
     * Returns the current number of users.
     *
     * @return the user count
     */
    public int size() {
        synchronized (stateLock) {
            return usersById.size();
        }
    }

    private void changeStatus(long userId, UserStatus status) {
        update(userId, user -> copy(user, user.username(), user.email(), user.roles(), status));
    }

    private User update(long userId, UnaryOperator<User> updater) {
        Objects.requireNonNull(updater, "updater");
        synchronized (stateLock) {
            User current = requireUser(userId);
            User updated = Objects.requireNonNull(updater.apply(current), "updated user");
            if (updated == current) {
                return current;
            }
            replace(current, ignored -> updated);
            return updated;
        }
    }

    private void replace(User current, UnaryOperator<User> updater) {
        User updated = Objects.requireNonNull(updater.apply(current), "updated user");
        usersById.put(current.id(), updated);
    }

    private User copy(
            User current,
            String username,
            @Nullable String email,
            Set<Role> roles,
            UserStatus status
    ) {
        Instant now = Objects.requireNonNull(clock.instant(), "clock instant");
        return new User(
                current.id(),
                username,
                email,
                roles,
                status,
                current.registeredAt(),
                now
        );
    }

    private User requireUser(long userId) {
        if (userId < 1L) {
            throw new IllegalArgumentException("userId must be positive");
        }
        User user = usersById.get(userId);
        if (user == null) {
            throw notFound();
        }
        return user;
    }

    private static ResourceReference<Long> requireUserReference(ResourceReference<Long> reference) {
        ResourceReference<Long> nonNullReference = Objects.requireNonNull(reference, "reference");
        if (!UserResourceTypes.USER.equals(nonNullReference.resourceType())) {
            throw new ResourceException(ResourceErrorCode.INVALID_REFERENCE, "Reference is not a user reference");
        }
        return nonNullReference;
    }

    private static ResourceException notFound() {
        return new ResourceException(ResourceErrorCode.NOT_FOUND, "User was not found");
    }

    private static String requireUsername(String username) {
        String nonNullUsername = Objects.requireNonNull(username, "username");
        if (nonNullUsername.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return nonNullUsername;
    }

    private List<User> snapshot() {
        synchronized (stateLock) {
            return List.copyOf(usersById.values());
        }
    }
}
