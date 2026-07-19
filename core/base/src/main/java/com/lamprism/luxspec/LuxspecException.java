package com.lamprism.luxspec;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A runtime exception that carries one stable business error code.
 *
 * @author RollW
 */
public class LuxspecException extends RuntimeException implements ErrorCodeCarrier {
    /**
     * The stable business error associated with this exception.
     */
    private final ErrorCode errorCode;

    /**
     * Creates an exception with no underlying cause.
     *
     * @param errorCode the stable business error code
     * @param message the safe failure message
     */
    public LuxspecException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * Creates an exception with an optional underlying cause.
     *
     * @param errorCode the stable business error code
     * @param message the safe failure message
     * @param cause the optional underlying failure
     */
    public LuxspecException(ErrorCode errorCode, String message, @Nullable Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
