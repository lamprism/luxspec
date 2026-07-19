package com.lamprism.luxspec.config;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Represents one raw source entry without using null as an entry state.
 *
 * @author RollW
 */
public final class ConfigEntry {
    public enum State {
        ABSENT,
        PRESENT,
        INVALID,
        MASKED
    }

    private static final ConfigEntry ABSENT = new ConfigEntry(State.ABSENT, null, null);
    private static final ConfigEntry MASKED = new ConfigEntry(State.MASKED, null, null);
    private final State state;
    private final RawConfigValue rawValue;
    private final String invalidDescription;

    private ConfigEntry(State state, @Nullable RawConfigValue rawValue, @Nullable String invalidDescription) {
        this.state = state;
        this.rawValue = rawValue;
        this.invalidDescription = invalidDescription;
    }

    /**
     * Returns the singleton state indicating that a source has no entry.
     *
     * @return the absent entry
     */
    public static ConfigEntry absent() {
        return ABSENT;
    }

    /**
     * Returns the singleton state that blocks lower-priority source resolution.
     *
     * @return the masked entry
     */
    public static ConfigEntry masked() {
        return MASKED;
    }

    public static ConfigEntry present(String rawValue) {
        return present(RawConfigValue.scalar(rawValue));
    }

    /**
     * Creates an entry that contains a raw list value.
     *
     * @param rawValues the raw list elements
     * @return the present entry
     */
    public static ConfigEntry present(java.util.List<String> rawValues) {
        return present(RawConfigValue.list(rawValues));
    }

    /**
     * Creates an entry that contains one provider-neutral raw value.
     *
     * @param rawValue the raw source value
     * @return the present entry
     */
    public static ConfigEntry present(RawConfigValue rawValue) {
        return new ConfigEntry(State.PRESENT, Objects.requireNonNull(rawValue, "rawValue"), null);
    }

    /**
     * Creates an entry whose source data cannot be represented or read safely.
     *
     * @param description a value-free failure description
     * @return the invalid entry
     */
    public static ConfigEntry invalid(String description) {
        if (Objects.requireNonNull(description, "description").isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return new ConfigEntry(State.INVALID, null, description);
    }

    /**
     * Returns the explicit entry state.
     *
     * @return the entry state
     */
    public State getState() {
        return state;
    }

    /**
     * Returns the raw value when this entry is present.
     *
     * @return the raw source value
     * @throws IllegalStateException when this entry is absent or masked
     */
    public RawConfigValue requireRawValue() {
        if (state != State.PRESENT) {
            throw new IllegalStateException("Entry does not contain a raw value");
        }
        return rawValue;
    }

    /**
     * Returns the value-free invalid-state description.
     *
     * @return the invalid description
     * @throws IllegalStateException when this entry is not invalid
     */
    public String requireInvalidDescription() {
        if (state != State.INVALID) {
            throw new IllegalStateException("Entry is not invalid");
        }
        return invalidDescription;
    }
}
