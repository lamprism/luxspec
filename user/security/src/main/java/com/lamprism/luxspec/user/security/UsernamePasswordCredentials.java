package com.lamprism.luxspec.user.security;

import com.lamprism.luxspec.security.authentication.Credentials;
import java.util.Objects;

/**
 * Carries one username and raw password for one synchronous authentication attempt.
 *
 * @author RollW
 */
public final class UsernamePasswordCredentials implements Credentials {
    private final String username;
    private final CharSequence rawPassword;

    /**
     * Creates username and password credentials.
     *
     * @param username the non-blank account name
     * @param rawPassword the raw password used only during authentication
     */
    public UsernamePasswordCredentials(String username, CharSequence rawPassword) {
        this.username = requireUsername(username);
        this.rawPassword = Objects.requireNonNull(rawPassword, "rawPassword");
    }

    /**
     * Returns the account name used for lookup.
     *
     * @return the non-blank username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the raw password only for immediate scheme processing.
     *
     * @return the raw password
     */
    public CharSequence getRawPassword() {
        return rawPassword;
    }

    @Override
    public String toString() {
        return "UsernamePasswordCredentials[username=" + username + ", rawPassword=redacted]";
    }

    private static String requireUsername(String value) {
        String username = Objects.requireNonNull(value, "username");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return username;
    }
}
