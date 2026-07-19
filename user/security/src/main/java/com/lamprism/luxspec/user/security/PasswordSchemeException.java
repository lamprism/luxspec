package com.lamprism.luxspec.user.security;

import com.lamprism.luxspec.LuxspecException;
import org.jspecify.annotations.Nullable;

/**
 * Indicates that a protected password representation could not be processed safely.
 *
 * @author RollW
 */
public final class PasswordSchemeException extends LuxspecException {
    /**
     * Creates a password-scheme failure without an underlying cause.
     *
     * @param errorCode the stable password-scheme error
     * @param message the trusted-backend failure message
     */
    public PasswordSchemeException(PasswordErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a password-scheme failure with an optional underlying cause.
     *
     * @param errorCode the stable password-scheme error
     * @param message the trusted-backend failure message
     * @param cause the optional internal cause
     */
    public PasswordSchemeException(PasswordErrorCode errorCode, String message, @Nullable Throwable cause) {
        super(errorCode, message, cause);
    }
}
