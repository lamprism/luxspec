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

import java.util.Objects;

/**
 * Immutable operational counters for one cache instance.
 *
 * <p>A provider may return zero-valued counters when statistics are disabled. The snapshot is
 * deliberately independent of a cache vendor so observability adapters can consume it without
 * depending on Caffeine.</p>
 *
 * @author RollW
 */
public final class CacheStatisticsSnapshot {
    private static final CacheStatisticsSnapshot EMPTY = new CacheStatisticsSnapshot(0L, 0L, 0L, 0L, 0L, 0L, 0L);

    private final long hitCount;
    private final long missCount;
    private final long loadSuccessCount;
    private final long loadFailureCount;
    private final long loadDurationNanos;
    private final long evictionCount;
    private final long currentSize;

    /**
     * Creates a validated cache statistics snapshot.
     *
     * @param hitCount          successful lookups
     * @param missCount         unsuccessful lookups
     * @param loadSuccessCount  successful loader executions
     * @param loadFailureCount  failed loader executions
     * @param loadDurationNanos cumulative loader duration in nanoseconds
     * @param evictionCount     evicted entries
     * @param currentSize       current estimated entry count
     */
    public CacheStatisticsSnapshot(
            long hitCount,
            long missCount,
            long loadSuccessCount,
            long loadFailureCount,
            long loadDurationNanos,
            long evictionCount,
            long currentSize
    ) {
        this.hitCount = nonNegative(hitCount, "hitCount");
        this.missCount = nonNegative(missCount, "missCount");
        this.loadSuccessCount = nonNegative(loadSuccessCount, "loadSuccessCount");
        this.loadFailureCount = nonNegative(loadFailureCount, "loadFailureCount");
        this.loadDurationNanos = nonNegative(loadDurationNanos, "loadDurationNanos");
        this.evictionCount = nonNegative(evictionCount, "evictionCount");
        this.currentSize = nonNegative(currentSize, "currentSize");
    }

    /**
     * Returns an empty statistics snapshot.
     *
     * @return the shared empty snapshot
     */
    public static CacheStatisticsSnapshot empty() {
        return EMPTY;
    }

    public long getHitCount() {
        return hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public long getLoadSuccessCount() {
        return loadSuccessCount;
    }

    public long getLoadFailureCount() {
        return loadFailureCount;
    }

    public long getLoadDurationNanos() {
        return loadDurationNanos;
    }

    public long getEvictionCount() {
        return evictionCount;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public long getRequestCount() {
        return hitCount + missCount;
    }

    public double getHitRate() {
        long requests = getRequestCount();
        return requests == 0L ? 1.0d : (double) hitCount / (double) requests;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CacheStatisticsSnapshot stats)) {
            return false;
        }
        return hitCount == stats.hitCount
                && missCount == stats.missCount
                && loadSuccessCount == stats.loadSuccessCount
                && loadFailureCount == stats.loadFailureCount
                && loadDurationNanos == stats.loadDurationNanos
                && evictionCount == stats.evictionCount
                && currentSize == stats.currentSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                hitCount,
                missCount,
                loadSuccessCount,
                loadFailureCount,
                loadDurationNanos,
                evictionCount,
                currentSize
        );
    }

    private static long nonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
