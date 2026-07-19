package com.lamprism.luxspec.user.persistence;

import com.lamprism.luxspec.user.security.EncodedPassword;
import com.lamprism.luxspec.user.security.UserPasswordStore;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and conditionally updates encoded local-user passwords through JPA.
 *
 * @author RollW
 */
public final class JpaUserPasswordStore implements UserPasswordStore {
    private final UserRepository repository;
    private final Clock clock;

    /**
     * Creates a password store with its repository and time source.
     *
     * @param repository the internal user repository
     * @param clock the time source for password upgrades
     */
    public JpaUserPasswordStore(UserRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Finds the configured encoded password for one user.
     *
     * @param userId the positive user identifier
     * @return the encoded password, or empty when no password is configured
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<EncodedPassword> find(long userId) {
        requireUserId(userId);
        return repository.findById(userId)
                .map(UserEntity::getPassword)
                .map(EncodedPassword::new);
    }

    /**
     * Replaces a password only when the expected current representation remains stored.
     *
     * @param userId the positive user identifier
     * @param currentPassword the representation previously verified by the authenticator
     * @param replacementPassword the newly encoded representation
     * @return whether the replacement changed one row
     */
    @Override
    @Transactional
    public boolean replace(
            long userId,
            EncodedPassword currentPassword,
            EncodedPassword replacementPassword
    ) {
        requireUserId(userId);
        EncodedPassword expected = Objects.requireNonNull(currentPassword, "currentPassword");
        EncodedPassword replacement = Objects.requireNonNull(replacementPassword, "replacementPassword");
        return repository.replacePassword(
                userId,
                expected.getValue(),
                replacement.getValue(),
                clock.instant()
        ) == 1;
    }

    private static void requireUserId(long userId) {
        if (userId < 1L) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
