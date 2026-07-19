package com.lamprism.luxspec.config;

import java.util.Objects;

/**
 * Reports an effective configuration change without exposing value bodies.
 *
 * @author RollW
 */
public final class ConfigChangedEvent {
    private final ConfigKey key;
    private final boolean sensitive;
    private final ResolvedConfig.Origin previousOrigin;
    private final boolean previousValuePresent;
    private final ResolvedConfig.Origin currentOrigin;
    private final boolean currentValuePresent;

    /**
     * Creates value-free metadata for one effective configuration change.
     *
     * @param spec the changed complete configuration definition
     * @param previous the previous effective resolution
     * @param current the current effective resolution
     */
    public ConfigChangedEvent(ConfigSpec<?> spec, ResolvedConfig<?> previous, ResolvedConfig<?> current) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        this.key = nonNullSpec.getKey();
        this.sensitive = nonNullSpec.isSensitive();
        this.previousOrigin = Objects.requireNonNull(previous, "previous").getOrigin();
        this.previousValuePresent = previous.getValue().isPresent();
        this.currentOrigin = Objects.requireNonNull(current, "current").getOrigin();
        this.currentValuePresent = current.getValue().isPresent();
    }

    /**
     * Returns the changed complete key.
     *
     * @return the changed key
     */
    public ConfigKey getKey() {
        return key;
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
     * Returns the origin before the mutation.
     *
     * @return the previous origin
     */
    public ResolvedConfig.Origin getPreviousOrigin() {
        return previousOrigin;
    }

    /**
     * Reports whether a value was effective before the mutation.
     *
     * @return {@code true} when a previous value was effective
     */
    public boolean isPreviousValuePresent() {
        return previousValuePresent;
    }

    /**
     * Returns the origin after the mutation.
     *
     * @return the current origin
     */
    public ResolvedConfig.Origin getCurrentOrigin() {
        return currentOrigin;
    }

    /**
     * Reports whether a value is effective after the mutation.
     *
     * @return {@code true} when a current value is effective
     */
    public boolean isCurrentValuePresent() {
        return currentValuePresent;
    }
}
