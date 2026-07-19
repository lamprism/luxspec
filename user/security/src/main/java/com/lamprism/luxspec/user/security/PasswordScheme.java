package com.lamprism.luxspec.user.security;

/**
 * Creates, verifies, and evaluates protected password representations.
 *
 * @author RollW
 */
public interface PasswordScheme {
    /**
     * Encodes one raw password for protected storage.
     *
     * @param rawPassword the raw password used only for this synchronous operation
     * @return the non-null opaque encoded password
     */
    EncodedPassword encode(CharSequence rawPassword);

    /**
     * Verifies one raw password against an encoded representation.
     *
     * @param rawPassword the raw password used only for this synchronous operation
     * @param encodedPassword the protected stored representation
     * @return whether the password matches
     * @throws PasswordSchemeException when the stored representation cannot be processed safely
     */
    boolean verify(CharSequence rawPassword, EncodedPassword encodedPassword);

    /**
     * Reports whether one stored representation should be replaced after successful verification.
     *
     * @param encodedPassword the protected stored representation
     * @return whether the representation is below the current protection parameters
     * @throws PasswordSchemeException when the stored representation cannot be processed safely
     */
    boolean needsUpgrade(EncodedPassword encodedPassword);
}
