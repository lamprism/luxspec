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

package com.lamprism.luxspec.data.query;

import java.time.Instant;
import java.util.Objects;

/**
 * One immutable timestamped data point.
 *
 * @param <T> the point value type
 * @author RollW
 */
public final class TimeSeriesPoint<T> {
    /**
     * The timestamp field shared by time-series query implementations.
     */
    public static final QueryField<Instant> TIMESTAMP = QueryField.of("timestamp", Instant.class);

    private final Instant timestamp;
    private final T value;

    /**
     * Creates one timestamped point.
     *
     * @param timestamp the point timestamp
     * @param value     the point value
     */
    public TimeSeriesPoint(Instant timestamp, T value) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Returns the point timestamp.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the point value.
     *
     * @return the value
     */
    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TimeSeriesPoint<?> point)) {
            return false;
        }
        return timestamp.equals(point.timestamp) && value.equals(point.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value);
    }
}
