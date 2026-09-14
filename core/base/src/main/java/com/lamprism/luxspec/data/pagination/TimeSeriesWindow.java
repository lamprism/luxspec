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

package com.lamprism.luxspec.data.pagination;

import java.time.Instant;
import java.util.Objects;

/**
 * Defines an inclusive time range and a bound for returned points.
 *
 * <p>The timestamp field is supplied by the query executor through the
 * standard {@code TimeSeriesPoint.TIMESTAMP} field. The window deliberately
 * does not define aggregation or resampling.</p>
 *
 * @author RollW
 */
public final class TimeSeriesWindow implements QueryWindow {
    private final Instant fromInclusive;
    private final Instant toInclusive;
    private final int maximumPoints;

    /**
     * Creates a bounded inclusive time window.
     *
     * @param fromInclusive the first included timestamp
     * @param toInclusive   the last included timestamp
     * @param maximumPoints the maximum number of returned points
     */
    public TimeSeriesWindow(Instant fromInclusive, Instant toInclusive, int maximumPoints) {
        Instant nonNullFrom = Objects.requireNonNull(fromInclusive, "fromInclusive");
        Instant nonNullTo = Objects.requireNonNull(toInclusive, "toInclusive");
        if (nonNullFrom.isAfter(nonNullTo)) {
            throw new IllegalArgumentException("fromInclusive must not be after toInclusive");
        }
        if (maximumPoints < 1) {
            throw new IllegalArgumentException("maximumPoints must be positive");
        }
        this.fromInclusive = nonNullFrom;
        this.toInclusive = nonNullTo;
        this.maximumPoints = maximumPoints;
    }

    /**
     * Returns the first included timestamp.
     *
     * @return the inclusive lower bound
     */
    public Instant fromInclusive() {
        return fromInclusive;
    }

    /**
     * Returns the last included timestamp.
     *
     * @return the inclusive upper bound
     */
    public Instant toInclusive() {
        return toInclusive;
    }

    /**
     * Returns the maximum number of returned points.
     *
     * @return the point limit
     */
    public int maximumPoints() {
        return maximumPoints;
    }

    /**
     * Reports whether a timestamp belongs to this window.
     *
     * @param timestamp the timestamp to test
     * @return true when the timestamp is within the inclusive range
     */
    public boolean contains(Instant timestamp) {
        Instant nonNullTimestamp = Objects.requireNonNull(timestamp, "timestamp");
        return !nonNullTimestamp.isBefore(fromInclusive) && !nonNullTimestamp.isAfter(toInclusive);
    }
}
