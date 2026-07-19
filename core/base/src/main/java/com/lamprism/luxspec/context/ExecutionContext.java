package com.lamprism.luxspec.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable carrier for typed request and execution values.
 *
 * @author RollW
 */
public final class ExecutionContext {
    private final Map<ContextKey<?>, Object> values;

    private ExecutionContext(Map<ContextKey<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    /**
     * Creates an empty immutable context.
     *
     * @return the empty context
     */
    public static ExecutionContext empty() {
        return new ExecutionContext(Map.of());
    }

    /**
     * Looks up one typed context value.
     *
     * @param key the context key
     * @param <T> the context value type
     * @return the value when present
     */
    public <T> Optional<T> get(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(key.getValueType().cast(value));
    }

    /**
     * Returns a copy containing a previously absent typed value.
     *
     * @param key the absent context key
     * @param value the value to add
     * @param <T> the context value type
     * @return a derived immutable context
     */
    public <T> ExecutionContext with(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        validateValue(key, value);
        if (values.containsKey(key)) {
            throw new IllegalStateException("ContextKey is already present: " + key.getName());
        }
        return copyWith(key, value);
    }

    /**
     * Returns a copy with an existing typed value replaced.
     *
     * @param key the present context key
     * @param value the replacement value
     * @param <T> the context value type
     * @return a derived immutable context
     */
    public <T> ExecutionContext replace(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        validateValue(key, value);
        if (!values.containsKey(key)) {
            throw new IllegalStateException("ContextKey is not present: " + key.getName());
        }
        return copyWith(key, value);
    }

    /**
     * Returns a copy without one key while leaving this context unchanged.
     *
     * @param key the key to remove
     * @return this context when absent, otherwise a derived context
     */
    public ExecutionContext without(ContextKey<?> key) {
        Objects.requireNonNull(key, "key");
        if (!values.containsKey(key)) {
            return this;
        }
        Map<ContextKey<?>, Object> copy = new LinkedHashMap<>(values);
        copy.remove(key);
        return new ExecutionContext(copy);
    }

    private <T> ExecutionContext copyWith(ContextKey<T> key, T value) {
        Map<ContextKey<?>, Object> copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new ExecutionContext(copy);
    }

    private <T> void validateValue(ContextKey<T> key, T value) {
        Objects.requireNonNull(value, "value");
        if (!key.getValueType().isInstance(value)) {
            throw new IllegalArgumentException("Context value does not match key type: " + key.getName());
        }
    }
}
