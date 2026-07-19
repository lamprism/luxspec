package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.LuxspecException;
import org.jspecify.annotations.Nullable;

/**
 * Indicates a trusted authentication failure with a stable internal code.
 *
 * @author RollW
 */
public class AuthenticationException extends LuxspecException {
    /**
     * Creates an authentication failure with no underlying cause.
     *
     * @param errorCode the stable authentication error
     * @param message the trusted-backend failure message
     */
    public AuthenticationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates an authentication failure with an optional internal cause.
     *
     * @param errorCode the stable authentication error
     * @param message the trusted-backend failure message
     * @param cause the optional internal cause
     */
    public AuthenticationException(ErrorCode errorCode, String message, @Nullable Throwable cause) {
        super(errorCode, message, cause);
    }
}
