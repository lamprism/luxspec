package com.lamprism.luxspec.web;

import com.lamprism.luxspec.ErrorCode;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Describes the stable result code and optional safe message of an HTTP response.
 *
 * @author RollW
 */
public final class ResponseStatus {
    private static final ResponseStatus SUCCESS = new ResponseStatus("success", null);

    private final String code;

    @Nullable
    private final String message;

    private ResponseStatus(String code, @Nullable String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = message;
    }

    /**
     * Creates a failed status from a stable business error.
     */
    public static ResponseStatus failure(ErrorCode errorCode, @Nullable String message) {
        Objects.requireNonNull(errorCode, "errorCode");
        return new ResponseStatus(errorCode.getCode(), message);
    }

    /**
     * Returns the JSON-visible stable result code.
     */
    public String code() {
        return code;
    }

    /**
     * Returns the optional safe response message.
     */
    @Nullable
    public String message() {
        return message;
    }

    /**
     * Returns the shared successful status.
     */
    public static ResponseStatus success() {
        return SUCCESS;
    }
}
