package com.lamprism.luxspec.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines runtime cache behavior for one resolved configuration key.
 *
 * @author RollW
 */
public final class ConfigCachePolicy {
    /**
     * Identifies the configured cache behavior.
     */
    public enum Mode {
        NONE,
        INVALIDATION,
        TIME_TO_LIVE
    }

    private static final ConfigCachePolicy NONE = new ConfigCachePolicy(Mode.NONE, null);
    private static final ConfigCachePolicy INVALIDATION = new ConfigCachePolicy(Mode.INVALIDATION, null);
    private final Mode mode;
    private final Duration timeToLive;

    private ConfigCachePolicy(Mode mode, Duration timeToLive) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.timeToLive = timeToLive;
    }

    /**
     * Returns a policy that always reads the underlying sources.
     *
     * @return the no-cache policy
     */
    public static ConfigCachePolicy none() {
        return NONE;
    }

    /**
     * Returns a policy that retains values until explicit invalidation.
     *
     * @return the invalidation cache policy
     */
    public static ConfigCachePolicy invalidation() {
        return INVALIDATION;
    }

    /**
     * Returns a policy that retains values for one positive duration.
     *
     * @param timeToLive the positive retention duration
     * @return the time-to-live cache policy
     */
    public static ConfigCachePolicy timeToLive(Duration timeToLive) {
        Duration nonNullDuration = Objects.requireNonNull(timeToLive, "timeToLive");
        if (nonNullDuration.isNegative() || nonNullDuration.isZero()) {
            throw new IllegalArgumentException("timeToLive must be positive");
        }
        return new ConfigCachePolicy(Mode.TIME_TO_LIVE, nonNullDuration);
    }

    /**
     * Returns the cache behavior.
     *
     * @return the cache mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Returns the configured time-to-live when this is a TTL policy.
     *
     * @return the positive TTL
     * @throws IllegalStateException when this policy is not time-to-live based
     */
    public Duration requireTimeToLive() {
        if (mode != Mode.TIME_TO_LIVE) {
            throw new IllegalStateException("Cache policy does not define a time-to-live");
        }
        return timeToLive;
    }
}
