package com.lamprism.luxspec.config.value;

import java.util.Objects;

/**
 * Indicates that a decoded or explicitly written configuration value violates its definition.
 *
 * <p>The detail must not include the value itself because a value may be sensitive.</p>
 *
 * @author RollW
 */
public final class ConfigValueValidationException extends IllegalArgumentException {
    /**
     * Creates a value validation failure.
     *
     * @param detail a value-free explanation
     */
    public ConfigValueValidationException(String detail) {
        super(requireDetail(detail));
    }

    /**
     * Creates a value validation failure with an underlying cause.
     *
     * @param detail a value-free explanation
     * @param cause  the underlying validation failure
     */
    public ConfigValueValidationException(String detail, Throwable cause) {
        super(requireDetail(detail), Objects.requireNonNull(cause, "cause"));
    }

    private static String requireDetail(String detail) {
        String nonBlankDetail = Objects.requireNonNull(detail, "detail");
        if (nonBlankDetail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return nonBlankDetail;
    }
}
