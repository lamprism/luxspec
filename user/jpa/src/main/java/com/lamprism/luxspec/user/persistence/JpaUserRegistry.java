package com.lamprism.luxspec.user.persistence;

import com.lamprism.luxspec.ErrorCodes;
import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserRegistry;
import com.lamprism.luxspec.user.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Performs user-specific registrations and mutations through internal JPA persistence.
 *
 * @author RollW
 */
public class JpaUserRegistry implements UserRegistry {
    private final UserRepository repository;
    private final Clock clock;

    /**
     * Creates a registry with its owning repository and application clock.
     *
     * @param repository the repository owned by the user feature
     * @param clock the time source for lifecycle changes
     */
    public JpaUserRegistry(UserRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public User register(String username, @Nullable String email, Set<Role> roles) {
        String nonNullUsername = UserEntity.requireUsername(username);
        if (repository.findByUsername(nonNullUsername).isPresent()) {
            throw new LuxspecException(ErrorCodes.of("user:username-exists"), "Username already exists");
        }
        UserEntity entity = UserEntity.register(nonNullUsername, email, roles, now());
        return repository.save(entity).toUser();
    }

    @Override
    @Transactional
    public void rename(long userId, String username) {
        String nonNullUsername = UserEntity.requireUsername(username);
        UserEntity entity = required(userId);
        if (repository.findByUsername(nonNullUsername).filter(candidate -> !candidate.hasId(userId)).isPresent()) {
            throw new LuxspecException(ErrorCodes.of("user:username-exists"), "Username already exists");
        }
        entity.rename(nonNullUsername, now());
    }

    @Override
    @Transactional
    public void changeEmail(long userId, @Nullable String email) {
        required(userId).changeEmail(email, now());
    }

    @Override
    @Transactional
    public void grantRole(long userId, Role role) {
        required(userId).grant(role, now());
    }

    @Override
    @Transactional
    public void revokeRole(long userId, Role role) {
        required(userId).revoke(role, now());
    }

    @Override
    @Transactional
    public void activate(long userId) {
        changeStatus(userId, UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void disable(long userId) {
        changeStatus(userId, UserStatus.DISABLED);
    }

    @Override
    @Transactional
    public void lock(long userId) {
        changeStatus(userId, UserStatus.LOCKED);
    }

    @Override
    @Transactional
    public void cancel(long userId) {
        changeStatus(userId, UserStatus.CANCELED);
    }

    private void changeStatus(long userId, UserStatus status) {
        required(userId).changeStatus(status, now());
    }

    private UserEntity required(long userId) {
        return repository.findById(userId).orElseThrow(() -> new LuxspecException(ErrorCodes.of("user:not-found"), "User was not found"));
    }

    private Instant now() {
        return clock.instant();
    }
}
