package com.lamprism.luxspec.context;

import java.util.Objects;

/**
 * Identifies one typed immutable ExecutionContext element.
 *
 * @param <T> the element type
 * @author RollW
 */
public final class ContextKey<T> {
    private final String name;
    private final Class<T> valueType;

    private ContextKey(String name, Class<T> valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    /**
     * Creates a typed immutable context-key identity.
     *
     * @param name the non-blank key name
     * @param valueType the context value type
     * @param <T> the context value type
     * @return the immutable context key
     */
    public static <T> ContextKey<T> of(String name, Class<T> valueType) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new ContextKey<>(name, Objects.requireNonNull(valueType, "valueType"));
    }

    /**
     * Returns the stable key name.
     *
     * @return the key name
     */
    public String getName() {
        return name;
    }

    Class<T> getValueType() {
        return valueType;
    }
}
