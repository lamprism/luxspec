package com.lamprism.luxspec.config.source.process;

import java.util.Objects;

/**
 * One configuration assignment parsed from a command-line argument position.
 *
 * @param argumentIndex    the zero-based source argument position
 * @param name             the complete external configuration name
 * @param value            the raw assignment value
 * @param hasExplicitValue whether the argument explicitly supplied a value
 * @author RollW
 */
public record CommandLineConfigAssignment(
        int argumentIndex,
        String name,
        String value,
        boolean hasExplicitValue
) {
    /**
     * Creates one validated command-line assignment.
     *
     * @param argumentIndex    the zero-based source argument position
     * @param name             the complete external configuration name
     * @param value            the raw assignment value
     * @param hasExplicitValue whether the argument explicitly supplied a value
     */
    public CommandLineConfigAssignment {
        if (argumentIndex < 0) {
            throw new IllegalArgumentException("argumentIndex must not be negative");
        }
        name = ProcessConfigName.requireName(name, "name");
        value = Objects.requireNonNull(value, "value");
    }
}
