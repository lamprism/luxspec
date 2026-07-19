package com.lamprism.luxspec.user.persistence;

import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Internal JPA mapping for one optional canonical local-user account.
 *
 * <p>The owning application applies this package's Liquibase changelog before using the mapping.</p>
 */
@Entity
@Table(
        name = "luxspec_user",
        uniqueConstraints = @UniqueConstraint(name = "ux_luxspec_user_username", columnNames = "username")
)
public class UserEntity {
    static final int USERNAME_LENGTH = 120;
    static final int PASSWORD_LENGTH = 255;
    static final int EMAIL_LENGTH = 320;
    static final int STATUS_LENGTH = 32;
    static final int ROLE_NAME_LENGTH = 128;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = USERNAME_LENGTH)
    private String username;

    @Column(length = PASSWORD_LENGTH)
    private String password;

    @Column(length = EMAIL_LENGTH)
    @Nullable
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_LENGTH)
    private UserStatus status;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "luxspec_user_role", joinColumns = @JoinColumn(name = "user_id", nullable = false))
    @Column(name = "role_name", nullable = false, length = ROLE_NAME_LENGTH)
    private Set<String> roles = new LinkedHashSet<>();

    /**
     * Creates an empty entity for JPA materialization.
     */
    protected UserEntity() {
    }

    static UserEntity register(String username, @Nullable String email, Set<Role> roles, Instant registeredAt) {
        UserEntity entity = new UserEntity();
        entity.username = requireUsername(username);
        entity.email = requireEmail(email);
        entity.status = UserStatus.ACTIVE;
        entity.registeredAt = registeredAt;
        entity.updatedAt = registeredAt;
        entity.replaceRoles(roles);
        return entity;
    }

    void rename(String username, Instant updatedAt) {
        this.username = requireUsername(username);
        this.updatedAt = updatedAt;
    }

    void changeEmail(@Nullable String email, Instant updatedAt) {
        this.email = requireEmail(email);
        this.updatedAt = updatedAt;
    }

    void grant(Role role, Instant updatedAt) {
        roles.add(requireRole(role).name());
        this.updatedAt = updatedAt;
    }

    void revoke(Role role, Instant updatedAt) {
        if (roles.size() == 1 && roles.contains(role.name())) {
            throw new IllegalStateException("A user must retain one role");
        }
        roles.remove(role.name());
        this.updatedAt = updatedAt;
    }

    void changeStatus(UserStatus status, Instant updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
    }

    boolean hasId(long expectedId) {
        return id == expectedId;
    }

    @Nullable
    String getPassword() {
        return password;
    }

    private void replaceRoles(Set<Role> values) {
        for (Role role : Objects.requireNonNull(values, "roles")) {
            roles.add(requireRole(role).name());
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("A user must have one role");
        }
    }

    User toUser() {
        Set<Role> mappedRoles = new LinkedHashSet<>();
        for (String role : roles) {
            mappedRoles.add(Role.of(role));
        }
        return new User(id, username, email, mappedRoles, status, registeredAt, updatedAt);
    }

    static String requireUsername(String value) {
        String username = Objects.requireNonNull(value, "username");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (username.length() > USERNAME_LENGTH) {
            throw new IllegalArgumentException("username exceeds the JPA storage limit");
        }
        return username;
    }

    private static @Nullable String requireEmail(@Nullable String value) {
        if (value != null && value.length() > EMAIL_LENGTH) {
            throw new IllegalArgumentException("email exceeds the JPA storage limit");
        }
        return value;
    }

    private static Role requireRole(Role value) {
        Role role = Objects.requireNonNull(value, "role");
        if (role.name().length() > ROLE_NAME_LENGTH) {
            throw new IllegalArgumentException("role name exceeds the JPA storage limit");
        }
        return role;
    }
}
