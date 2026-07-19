package com.lamprism.luxspec.user;

import com.lamprism.luxspec.resource.Resource;
import com.lamprism.luxspec.resource.ResourceReference;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * An immutable generic user account read model.
 *
 * @author RollW
 */
public final class User implements Resource<Long> {
    private final long id;
    private final String username;
    private final @Nullable String email;
    private final Set<Role> roles;
    private final UserStatus status;
    private final Instant registeredAt;
    private final Instant updatedAt;

    /**
     * Creates an immutable user read model.
     *
     * @param id the positive account identifier
     * @param username the non-blank unique account name
     * @param email the optional account email address
     * @param roles the non-empty assigned roles
     * @param status the account lifecycle status
     * @param registeredAt the account registration time
     * @param updatedAt the last account update time
     */
    public User(
            long id,
            String username,
            @Nullable String email,
            Set<Role> roles,
            UserStatus status,
            Instant registeredAt,
            Instant updatedAt
    ) {
        if (id < 1L) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.username = Objects.requireNonNull(username, "username");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        this.roles = Set.copyOf(roles);
        if (this.roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        this.id = id;
        this.email = email;
        this.status = Objects.requireNonNull(status, "status");
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Returns the positive account identifier.
     *
     * @return the account identifier
     */
    public long id() {
        return id;
    }

    /**
     * Returns the unique account name.
     *
     * @return the non-blank account name
     */
    public String username() {
        return username;
    }

    /**
     * Returns the optional account email address.
     *
     * @return the email address, or {@code null} when no email is assigned
     */
    public @Nullable String email() {
        return email;
    }

    /**
     * Returns the immutable assigned role set.
     *
     * @return the non-empty assigned roles
     */
    public Set<Role> roles() {
        return roles;
    }

    /**
     * Returns the account lifecycle status.
     *
     * @return the current lifecycle status
     */
    public UserStatus status() {
        return status;
    }

    /**
     * Returns the account registration time.
     *
     * @return the registration time
     */
    public Instant registeredAt() {
        return registeredAt;
    }

    /**
     * Returns the last account update time.
     *
     * @return the update time
     */
    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * Returns the resource reference for this account.
     *
     * @return the user resource reference
     */
    @Override
    public ResourceReference<Long> getReference() {
        return new ResourceReference<>(UserResourceTypes.USER, id);
    }
}
