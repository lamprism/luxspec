package com.lamprism.luxspec.database;

import java.time.Duration;
import java.util.Objects;

/**
 * Provider-neutral connection-pool policy.
 */
public final class ConnectionPoolPolicy {
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 10;
    private static final int DEFAULT_MINIMUM_IDLE = 10;
    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration DEFAULT_MAXIMUM_LIFETIME = Duration.ofMinutes(30);
    private static final Duration DEFAULT_LEAK_DETECTION_THRESHOLD = Duration.ZERO;

    private final int maximumPoolSize;
    private final int minimumIdle;
    private final Duration connectionTimeout;
    private final Duration idleTimeout;
    private final Duration maximumLifetime;
    private final Duration leakDetectionThreshold;

    private ConnectionPoolPolicy(Builder builder) {
        this.maximumPoolSize = builder.maximumPoolSize;
        this.minimumIdle = builder.minimumIdle;
        this.connectionTimeout = builder.connectionTimeout;
        this.idleTimeout = builder.idleTimeout;
        this.maximumLifetime = builder.maximumLifetime;
        this.leakDetectionThreshold = builder.leakDetectionThreshold;
        validate();
    }

    public static ConnectionPoolPolicy defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public Duration getMaximumLifetime() {
        return maximumLifetime;
    }

    public Duration getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    private void validate() {
        if (maximumPoolSize < 1) {
            throw new IllegalArgumentException("maximumPoolSize must be positive");
        }
        if (minimumIdle < 0 || minimumIdle > maximumPoolSize) {
            throw new IllegalArgumentException("minimumIdle must be between zero and maximumPoolSize");
        }
        validateDuration(connectionTimeout, "connectionTimeout", false, 250);
        validateDuration(idleTimeout, "idleTimeout", true, 10_000);
        validateDuration(maximumLifetime, "maximumLifetime", true, 30_000);
        validateDuration(leakDetectionThreshold, "leakDetectionThreshold", true, 2_000);
    }

    private static void validateDuration(
            Duration duration,
            String name,
            boolean zeroAllowed,
            long minimumMillis
    ) {
        Objects.requireNonNull(duration, name);
        if (duration.isNegative() || (!zeroAllowed && duration.isZero())) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        if (!duration.isZero() && duration.toMillis() < minimumMillis) {
            throw new IllegalArgumentException(name + " is below the supported minimum");
        }
    }

    public static final class Builder {
        private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
        private int minimumIdle = DEFAULT_MINIMUM_IDLE;
        private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private Duration idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private Duration maximumLifetime = DEFAULT_MAXIMUM_LIFETIME;
        private Duration leakDetectionThreshold = DEFAULT_LEAK_DETECTION_THRESHOLD;

        private Builder() {
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder minimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = Objects.requireNonNull(connectionTimeout, "connectionTimeout");
            return this;
        }

        public Builder idleTimeout(Duration idleTimeout) {
            this.idleTimeout = Objects.requireNonNull(idleTimeout, "idleTimeout");
            return this;
        }

        public Builder maximumLifetime(Duration maximumLifetime) {
            this.maximumLifetime = Objects.requireNonNull(maximumLifetime, "maximumLifetime");
            return this;
        }

        public Builder leakDetectionThreshold(Duration leakDetectionThreshold) {
            this.leakDetectionThreshold = Objects.requireNonNull(
                    leakDetectionThreshold,
                    "leakDetectionThreshold"
            );
            return this;
        }

        public ConnectionPoolPolicy build() {
            return new ConnectionPoolPolicy(this);
        }
    }

    @Override
    public String toString() {
        return "ConnectionPoolPolicy[maximumPoolSize=" + maximumPoolSize
                + ", minimumIdle=" + minimumIdle
                + ", connectionTimeout=" + connectionTimeout
                + ", idleTimeout=" + idleTimeout
                + ", maximumLifetime=" + maximumLifetime
                + ", leakDetectionThreshold=" + leakDetectionThreshold
                + "]";
    }
}
