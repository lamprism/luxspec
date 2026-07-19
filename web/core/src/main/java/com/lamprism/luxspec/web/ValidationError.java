package com.lamprism.luxspec.web;

import com.lamprism.luxspec.ErrorCode;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Describes one safe structured validation failure.
 *
 * @author RollW
 */
public final class ValidationError {
    private final String field;
    private final ErrorCode code;
    private final String message;

    public ValidationError(String field, ErrorCode code, @Nullable String message) {
        Objects.requireNonNull(field, "field");
        if (field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        this.field = field;
        this.code = Objects.requireNonNull(code, "code");
        this.message = message;
    }

    public String field() {
        return field;
    }
    public ErrorCode code() {
        return code;
    }
    public @Nullable String message() {
        return message;
    }
}
