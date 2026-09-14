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

package com.lamprism.luxspec.observability.metric;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of one metric collection pass.
 *
 * <p>The capture time, non-negative sequence, and reading list are required.
 * The list is copied at construction, and nullable fields on individual
 * readings represent unavailable or instrument-specific measurements rather
 * than fabricated zero values.
 *
 * @author RollW
 */
public final class MetricSnapshot {
    private final Instant capturedAt;
    private final long sequence;
    private final List<MetricReading> readings;

    public MetricSnapshot(Instant capturedAt, long sequence, List<MetricReading> readings) {
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
        if (sequence < 0L) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        this.sequence = sequence;
        this.readings = List.copyOf(Objects.requireNonNull(readings, "readings"));
    }

    public Instant capturedAt() {
        return capturedAt;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public long sequence() {
        return sequence;
    }

    public long getSequence() {
        return sequence;
    }

    public List<MetricReading> readings() {
        return readings;
    }

    public List<MetricReading> getReadings() {
        return readings;
    }
}
