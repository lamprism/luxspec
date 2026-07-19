package com.lamprism.luxspec.user.spring;

import com.lamprism.luxspec.user.security.EncodedPassword;
import com.lamprism.luxspec.user.security.PasswordScheme;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Adapts the provider-independent Luxspec password scheme to Spring Security.
 *
 * @author RollW
 */
public final class LuxspecPasswordEncoder implements PasswordEncoder {
    private final PasswordScheme passwordScheme;

    /**
     * Creates a Spring password encoder backed by one Luxspec password scheme.
     *
     * @param passwordScheme the authoritative Luxspec password scheme
     */
    public LuxspecPasswordEncoder(PasswordScheme passwordScheme) {
        this.passwordScheme = Objects.requireNonNull(passwordScheme, "passwordScheme");
    }

    /**
     * Encodes one raw password through the configured Luxspec scheme.
     *
     * @param rawPassword the raw password
     * @return the opaque encoded password value
     */
    @Override
    public String encode(CharSequence rawPassword) {
        return passwordScheme.encode(rawPassword).getValue();
    }

    /**
     * Verifies one raw password through the configured Luxspec scheme.
     *
     * @param rawPassword the raw password
     * @param encodedPassword the stored encoded password value
     * @return whether the password matches
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordScheme.verify(rawPassword, new EncodedPassword(encodedPassword));
    }

    /**
     * Reports whether one stored password should be re-encoded.
     *
     * @param encodedPassword the stored encoded password value
     * @return whether the configured scheme considers it outdated
     */
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return passwordScheme.needsUpgrade(new EncodedPassword(encodedPassword));
    }
}
