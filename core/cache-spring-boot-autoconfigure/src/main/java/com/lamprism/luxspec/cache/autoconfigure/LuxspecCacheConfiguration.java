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

package com.lamprism.luxspec.cache.autoconfigure;

import com.lamprism.luxspec.cache.CacheProfile;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bounded local-cache settings for the default Caffeine adapter.
 *
 * @author RollW
 */
@ConfigurationProperties("luxspec.cache")
public class LuxspecCacheConfiguration {
    private long maximumSize = 1_024;
    private @Nullable Duration expireAfterWrite;
    private @Nullable Duration expireAfterAccess;
    private boolean recordStats;

    /**
     * Returns the maximum number of entries.
     *
     * @return the maximum entry count
     */
    public long getMaximumSize() {
        return maximumSize;
    }

    /**
     * Sets the maximum number of entries.
     *
     * @param maximumSize the maximum entry count
     */
    public void setMaximumSize(long maximumSize) {
        this.maximumSize = maximumSize;
    }

    /**
     * Returns the write-expiration duration.
     *
     * @return the optional duration
     */
    public @Nullable Duration getExpireAfterWrite() {
        return expireAfterWrite;
    }

    /**
     * Sets the write-expiration duration.
     *
     * @param expireAfterWrite the optional duration
     */
    public void setExpireAfterWrite(@Nullable Duration expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    /**
     * Returns the access-expiration duration.
     *
     * @return the optional duration
     */
    public @Nullable Duration getExpireAfterAccess() {
        return expireAfterAccess;
    }

    /**
     * Sets the access-expiration duration.
     *
     * @param expireAfterAccess the optional duration
     */
    public void setExpireAfterAccess(@Nullable Duration expireAfterAccess) {
        this.expireAfterAccess = expireAfterAccess;
    }

    public boolean isRecordStats() {
        return recordStats;
    }

    public void setRecordStats(boolean recordStats) {
        this.recordStats = recordStats;
    }

    /**
     * Converts bound properties to provider-independent cache controls.
     *
     * @return the validated cache profile
     */
    public CacheProfile toCacheProfile() {
        CacheProfile.Builder builder = CacheProfile.builder()
                .maximumSize(maximumSize);
        if (expireAfterWrite != null) {
            builder.expireAfterWrite(expireAfterWrite);
        }
        if (expireAfterAccess != null) {
            builder.expireAfterAccess(expireAfterAccess);
        }
        builder.recordStats(recordStats);
        return builder.build();
    }
}
