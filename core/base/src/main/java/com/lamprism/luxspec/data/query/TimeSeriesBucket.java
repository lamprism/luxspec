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

import java.time.Duration;
import java.time.Instant;

/**
 * Assigns an instant to an inclusive time-series bucket start.
 *
 * <p>Implementations must be deterministic and safe for concurrent use when a plan is shared.
 * The returned start must not be after the supplied timestamp.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface TimeSeriesBucket {
    /**
     * Creates fixed-width buckets anchored at the Unix epoch.
     *
     * @param size the positive bucket width
     * @return the fixed-width bucket definition
     */
    static FixedTimeSeriesBucket of(Duration size) {
        return new FixedTimeSeriesBucket(size, Instant.EPOCH);
    }

    /**
     * Creates fixed-width buckets anchored at an explicit instant.
     *
     * @param size the positive bucket width
     * @param origin the bucket origin
     * @return the fixed-width bucket definition
     */
    static FixedTimeSeriesBucket of(Duration size, Instant origin) {
        return new FixedTimeSeriesBucket(size, origin);
    }

    /**
     * Returns the inclusive bucket start containing the timestamp.
     *
     * @param timestamp the timestamp to assign
     * @return the bucket start
     */
    Instant start(Instant timestamp);
}
