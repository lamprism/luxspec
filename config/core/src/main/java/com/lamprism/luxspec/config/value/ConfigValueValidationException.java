package com.lamprism.luxspec.config.value;

import com.lamprism.luxspec.validation.ValidationException;

/**
 * Indicates that a decoded or explicitly written configuration value violates its definition.
 *
 * <p>The detail must not include the value itself because a value may be sensitive.</p>
 *
 * @author RollW
 */
public final class ConfigValueValidationException extends ValidationException {
    /**
     * Creates a value validation failure.
     *
     * @param detail a value-free explanation
     */
    public ConfigValueValidationException(String detail) {
        super(detail);
    }

    /**
     * Creates a value validation failure with an underlying cause.
     *
     * @param detail a value-free explanation
     * @param cause  the underlying validation failure
     */
    public ConfigValueValidationException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
