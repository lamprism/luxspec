package com.lamprism.luxspec.user;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Owns generic account registration and lifecycle mutations.
 *
 * @author RollW
 */
public interface UserRegistry {
    /**
     * Registers a user with a non-empty initial role set.
     *
     * @param username the unique non-blank account name
     * @param email the optional account email address
     * @param roles the non-empty initial roles
     * @return the registered user
     */
    User register(String username, @Nullable String email, Set<Role> roles);

    /**
     * Changes a user's unique account name.
     *
     * @param userId the user identifier
     * @param username the new non-blank unique account name
     */
    void rename(long userId, String username);

    /**
     * Changes a user's optional email address.
     *
     * @param userId the user identifier
     * @param email the email address, or {@code null} to remove it
     */
    void changeEmail(long userId, @Nullable String email);

    /**
     * Adds one role to a user.
     *
     * @param userId the user identifier
     * @param role the role to assign
     */
    void grantRole(long userId, Role role);

    /**
     * Removes one role when another role remains assigned.
     *
     * @param userId the user identifier
     * @param role the role to remove
     */
    void revokeRole(long userId, Role role);

    /**
     * Changes the user lifecycle state to active.
     *
     * @param userId the user identifier
     */
    void activate(long userId);

    /**
     * Changes the user lifecycle state to disabled.
     *
     * @param userId the user identifier
     */
    void disable(long userId);

    /**
     * Changes the user lifecycle state to locked.
     *
     * @param userId the user identifier
     */
    void lock(long userId);

    /**
     * Changes the user lifecycle state to canceled.
     *
     * @param userId the user identifier
     */
    void cancel(long userId);
}
