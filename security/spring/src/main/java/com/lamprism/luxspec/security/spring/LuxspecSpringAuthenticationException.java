package com.lamprism.luxspec.security.spring;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.ErrorCodeCarrier;
import java.util.Objects;

/**
 * Adapts a trusted Luxspec authentication failure for Spring Security entry points.
 *
 * @author RollW
 */
public final class LuxspecSpringAuthenticationException
        extends org.springframework.security.core.AuthenticationException
        implements ErrorCodeCarrier {
    /**
     * The stable Luxspec authentication error preserved for adapter consumers.
     */
    private final ErrorCode errorCode;

    /**
     * Creates a generic Spring Security failure while preserving the stable Luxspec error code.
     *
     * @param cause the trusted Luxspec authentication failure
     */
    public LuxspecSpringAuthenticationException(com.lamprism.luxspec.security.authentication.AuthenticationException cause) {
        super("Access token was rejected", Objects.requireNonNull(cause, "cause"));
        this.errorCode = cause.getErrorCode();
    }

    /**
     * Returns the stable Luxspec authentication error code.
     *
     * @return the error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
