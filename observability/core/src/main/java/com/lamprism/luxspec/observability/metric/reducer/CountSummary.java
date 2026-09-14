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

package com.lamprism.luxspec.observability.metric.reducer;

import org.jspecify.annotations.Nullable;

/**
 * Delta and rate values calculated from a cumulative metric count.
 *
 * <p>A decrease is treated as a counter reset. The count after the reset is counted as the
 * increment since that reset.</p>
 *
 * @author RollW
 */
public final class CountSummary {
    private final int sampleCount;
    private final @Nullable Long firstValue;
    private final @Nullable Long lastValue;
    private final @Nullable Long delta;
    private final @Nullable Double ratePerSecond;
    private final int resetCount;

    CountSummary(
            int sampleCount,
            @Nullable Long firstValue,
            @Nullable Long lastValue,
            @Nullable Long delta,
            @Nullable Double ratePerSecond,
            int resetCount
    ) {
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount must not be negative");
        }
        if (resetCount < 0) {
            throw new IllegalArgumentException("resetCount must not be negative");
        }
        this.sampleCount = sampleCount;
        this.firstValue = firstValue;
        this.lastValue = lastValue;
        this.delta = delta;
        this.ratePerSecond = ratePerSecond;
        this.resetCount = resetCount;
    }

    /**
     * Returns the number of samples with an available count.
     *
     * @return the available count value count
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Returns the first cumulative count in chronological order.
     *
     * @return the first count, or null when no count was available
     */
    public @Nullable Long getFirstValue() {
        return firstValue;
    }

    /**
     * Returns the last cumulative count in chronological order.
     *
     * @return the last count, or null when no count was available
     */
    public @Nullable Long getLastValue() {
        return lastValue;
    }

    /**
     * Returns the reset-aware cumulative count delta.
     *
     * @return the delta, or null when no count was available
     */
    public @Nullable Long getDelta() {
        return delta;
    }

    /**
     * Returns the delta divided by the elapsed seconds between the first and last count.
     *
     * @return the per-second rate, or null when fewer than two counts or no positive elapsed time
     * was available
     */
    public @Nullable Double getRatePerSecond() {
        return ratePerSecond;
    }

    /**
     * Returns the number of detected count decreases.
     *
     * @return the reset count
     */
    public int getResetCount() {
        return resetCount;
    }
}
