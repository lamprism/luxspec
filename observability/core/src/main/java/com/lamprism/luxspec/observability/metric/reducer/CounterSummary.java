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
 * Delta and rate values calculated from one cumulative counter series.
 *
 * <p>A decrease is treated as a counter reset. The value after the reset is counted as the
 * increment since that reset.</p>
 *
 * @author RollW
 */
public final class CounterSummary {
    private final int sampleCount;
    private final @Nullable Double firstValue;
    private final @Nullable Double lastValue;
    private final @Nullable Double delta;
    private final @Nullable Double ratePerSecond;
    private final int resetCount;

    CounterSummary(
            int sampleCount,
            @Nullable Double firstValue,
            @Nullable Double lastValue,
            @Nullable Double delta,
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
     * Returns the number of samples with an available counter value.
     *
     * @return the available counter value count
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Returns the first counter value in chronological order.
     *
     * @return the first value, or null when no value was available
     */
    public @Nullable Double getFirstValue() {
        return firstValue;
    }

    /**
     * Returns the last counter value in chronological order.
     *
     * @return the last value, or null when no value was available
     */
    public @Nullable Double getLastValue() {
        return lastValue;
    }

    /**
     * Returns the reset-aware cumulative delta.
     *
     * @return the delta, or null when no value was available
     */
    public @Nullable Double getDelta() {
        return delta;
    }

    /**
     * Returns the delta divided by the elapsed seconds between the first and last value.
     *
     * @return the per-second rate, or null when fewer than two values or no positive elapsed time
     * was available
     */
    public @Nullable Double getRatePerSecond() {
        return ratePerSecond;
    }

    /**
     * Returns the number of detected counter decreases.
     *
     * @return the reset count
     */
    public int getResetCount() {
        return resetCount;
    }
}
