package com.lamprism.luxspec.config.value;

import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

final class ImmutableConfigValue<T> implements ConfigValue<T> {
    private final State state;
    private final @Nullable T value;
    private final ConfigValueOrigin origin;

    private ImmutableConfigValue(
            State state,
            @Nullable T value,
            ConfigValueOrigin origin
    ) {
        this.state = Objects.requireNonNull(state, "state");
        this.value = value;
        this.origin = Objects.requireNonNull(origin, "origin");
        validateState();
    }

    static <T> ConfigValue<T> of(
            State state,
            @Nullable T value,
            ConfigValueOrigin origin
    ) {
        return new ImmutableConfigValue<>(state, value, origin);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public @Nullable T getValue() {
        return value;
    }

    @Override
    public ConfigValueOrigin getOrigin() {
        return origin;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigValue<?> configValue)) {
            return false;
        }
        return state == configValue.getState()
                && Objects.equals(value, configValue.getValue())
                && origin.equals(configValue.getOrigin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, value, origin);
    }

    @Override
    public String toString() {
        return "ConfigValue[state=" + state + ", origin=" + origin + "]";
    }

    private void validateState() {
        if (state == State.PRESENT && value == null) {
            throw new IllegalArgumentException("Present configuration values must contain a value");
        }
        if (state != State.PRESENT && value != null) {
            throw new IllegalArgumentException("Only present configuration values may contain a value");
        }
        if (state == State.ABSENT && origin instanceof ConfigValueOrigin.DefaultOrigin) {
            throw new IllegalArgumentException("Absent configuration values cannot use the default origin");
        }
        if (state == State.PRESENT && !(origin instanceof ConfigValueOrigin.SourceOrigin
                || origin instanceof ConfigValueOrigin.DefaultOrigin)) {
            throw new IllegalArgumentException("Present configuration values require a source or default origin");
        }
    }
}
