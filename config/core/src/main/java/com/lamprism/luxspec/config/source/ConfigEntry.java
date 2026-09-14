package com.lamprism.luxspec.config.source;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents one raw source entry without using null as an entry state.
 *
 * @author RollW
 */
public final class ConfigEntry {
    /**
     * Identifies the raw state stored by a source.
     */
    public enum State {
        /**
         * The source has no entry for the key.
         */
        ABSENT,
        /**
         * The source contains a raw value.
         */
        PRESENT,
        /**
         * The source entry could not be represented or read safely.
         */
        INVALID,
        /**
         * The source intentionally suppresses lower-priority values.
         */
        TOMBSTONE
    }

    private static final ConfigEntry ABSENT = new ConfigEntry(State.ABSENT, null, null);
    private static final ConfigEntry TOMBSTONE = new ConfigEntry(State.TOMBSTONE, null, null);
    private final State state;
    private final @Nullable RawConfigValue rawValue;
    private final @Nullable String invalidDescription;

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
     * Returns the singleton tombstone that suppresses lower-priority resolution.
     *
     * @return the tombstone entry
     */
    public static ConfigEntry tombstone() {
        return TOMBSTONE;
    }

    /**
     * Creates a raw string entry.
     *
     * @param rawValue the raw string
     * @return the present entry
     */
    public static ConfigEntry present(String rawValue) {
        return present(RawConfigValue.string(rawValue));
    }

    /**
     * Creates a raw string list entry.
     *
     * @param rawValues the raw list elements
     * @return the present entry
     */
    public static ConfigEntry present(List<String> rawValues) {
        return present(RawConfigValue.stringList(rawValues));
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
        String nonBlankDescription = Objects.requireNonNull(description, "description");
        if (nonBlankDescription.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return new ConfigEntry(State.INVALID, null, nonBlankDescription);
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
     * @throws IllegalStateException when this entry is not present
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
