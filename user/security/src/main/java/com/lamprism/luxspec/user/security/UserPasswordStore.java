package com.lamprism.luxspec.user.security;

import java.util.Optional;

/**
 * Reads and conditionally replaces protected password representations for user accounts.
 *
 * @author RollW
 */
public interface UserPasswordStore {
    /**
     * Finds the configured password for one user.
     *
     * @param userId the positive user identifier
     * @return the protected password, or empty when password authentication is not configured
     */
    Optional<EncodedPassword> find(long userId);

    /**
     * Replaces a password only when the verified current representation still matches.
     *
     * @param userId the positive user identifier
     * @param currentPassword the representation that was verified
     * @param replacementPassword the newly encoded representation
     * @return whether the replacement was applied
     */
    boolean replace(long userId, EncodedPassword currentPassword, EncodedPassword replacementPassword);
}
