/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.cache;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Generic bounded local-cache controls independent of a cache vendor.
 *
 * @author RollW
 */
public final class CacheProfile {
    private static final long DEFAULT_MAXIMUM_SIZE = 1_024;
    private final long maximumSize;
    private final @Nullable Duration expireAfterWrite;
    private final @Nullable Duration expireAfterAccess;

    private CacheProfile(
            long maximumSize,
            @Nullable Duration expireAfterWrite,
            @Nullable Duration expireAfterAccess
    ) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        this.maximumSize = maximumSize;
        this.expireAfterWrite = expireAfterWrite;
        this.expireAfterAccess = expireAfterAccess;
    }

    /**
     * Returns the default bounded profile.
     *
     * @return the default profile
     */
    public static CacheProfile defaults() {
        return builder().build();
    }

    /**
     * Creates a profile builder.
     *
     * @return the profile builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the maximum number of entries.
     *
     * @return the positive maximum size
     */
    public long getMaximumSize() {
        return maximumSize;
    }

    /**
     * Returns the write-expiration duration.
     *
     * @return the duration, or {@code null} when disabled
     */
    public @Nullable Duration getExpireAfterWrite() {
        return expireAfterWrite;
    }

    /**
     * Returns the access-expiration duration.
     *
     * @return the duration, or {@code null} when disabled
     */
    public @Nullable Duration getExpireAfterAccess() {
        return expireAfterAccess;
    }

    /**
     * Builds immutable generic cache controls.
     */
    public static final class Builder {
        private long maximumSize = DEFAULT_MAXIMUM_SIZE;
        private @Nullable Duration expireAfterWrite;
        private @Nullable Duration expireAfterAccess;

        private Builder() {
        }

        /**
         * Sets the maximum entry count.
         *
         * @param maximumSize the positive maximum entry count
         * @return this builder
         */
        public Builder maximumSize(long maximumSize) {
            if (maximumSize <= 0) {
                throw new IllegalArgumentException("maximumSize must be positive");
            }
            this.maximumSize = maximumSize;
            return this;
        }

        /**
         * Sets write-based expiration.
         *
         * @param duration the positive expiration duration
         * @return this builder
         */
        public Builder expireAfterWrite(Duration duration) {
            this.expireAfterWrite = positiveDuration(duration, "expireAfterWrite");
            return this;
        }

        /**
         * Sets access-based expiration.
         *
         * @param duration the positive expiration duration
         * @return this builder
         */
        public Builder expireAfterAccess(Duration duration) {
            this.expireAfterAccess = positiveDuration(duration, "expireAfterAccess");
            return this;
        }

        /**
         * Builds the immutable profile.
         *
         * @return the cache profile
         */
        public CacheProfile build() {
            return new CacheProfile(maximumSize, expireAfterWrite, expireAfterAccess);
        }

        private static Duration positiveDuration(Duration duration, String name) {
            Duration nonNullDuration = Objects.requireNonNull(duration, name);
            if (nonNullDuration.isZero() || nonNullDuration.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return nonNullDuration;
        }
    }
}
