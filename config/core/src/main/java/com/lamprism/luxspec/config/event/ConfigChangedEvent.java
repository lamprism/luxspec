package com.lamprism.luxspec.config.event;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;

import java.util.Objects;

/**
 * Reports an effective configuration change without exposing value bodies.
 *
 * @author RollW
 */
public final class ConfigChangedEvent implements ConfigEvent {
    private final ConfigKey key;
    private final boolean sensitive;
    private final ConfigValue.State previousState;
    private final ConfigValueOrigin previousOrigin;
    private final ConfigValue.State currentState;
    private final ConfigValueOrigin currentOrigin;

    /**
     * Creates value-free metadata for one effective configuration change.
     *
     * @param binding  the changed configuration binding
     * @param previous the previous effective value
     * @param current  the current effective value
     */
    public ConfigChangedEvent(
            ConfigBinding<?> binding,
            ConfigValue<?> previous,
            ConfigValue<?> current
    ) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        ConfigValue<?> nonNullPrevious = Objects.requireNonNull(previous, "previous");
        ConfigValue<?> nonNullCurrent = Objects.requireNonNull(current, "current");
        this.key = nonNullBinding.getKey();
        this.sensitive = nonNullBinding.getSpec().isSensitive();
        this.previousState = nonNullPrevious.getState();
        this.previousOrigin = nonNullPrevious.getOrigin();
        this.currentState = nonNullCurrent.getState();
        this.currentOrigin = nonNullCurrent.getOrigin();
    }

    @Override
    public ConfigKey getKey() {
        return key;
    }

    @Override
    public Kind getKind() {
        return Kind.EFFECTIVE_CHANGED;
    }

    /**
     * Reports whether the changed definition is sensitive.
     *
     * @return {@code true} when values must remain hidden
     */
    public boolean isSensitive() {
        return sensitive;
    }

    /**
     * Returns the previous result state.
     *
     * @return the previous state
     */
    public ConfigValue.State getPreviousState() {
        return previousState;
    }

    /**
     * Returns the origin before the mutation.
     *
     * @return the previous origin
     */
    public ConfigValueOrigin getPreviousOrigin() {
        return previousOrigin;
    }

    /**
     * Reports whether a value was effective before the mutation.
     *
     * @return {@code true} when a previous value was effective
     */
    public boolean isPreviousValuePresent() {
        return previousState == ConfigValue.State.PRESENT;
    }

    /**
     * Returns the current result state.
     *
     * @return the current state
     */
    public ConfigValue.State getCurrentState() {
        return currentState;
    }

    /**
     * Returns the origin after the mutation.
     *
     * @return the current origin
     */
    public ConfigValueOrigin getCurrentOrigin() {
        return currentOrigin;
    }

    /**
     * Reports whether a value is effective after the mutation.
     *
     * @return {@code true} when a current value is effective
     */
    public boolean isCurrentValuePresent() {
        return currentState == ConfigValue.State.PRESENT;
    }
}
