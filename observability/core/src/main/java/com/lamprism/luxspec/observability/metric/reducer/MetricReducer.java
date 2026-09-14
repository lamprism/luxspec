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

import com.lamprism.luxspec.observability.metric.MetricSample;

import java.util.Collection;

/**
 * Projects raw samples from one metric series into a typed result.
 *
 * <p>A reducer owns the projection rules for unavailable fields, ordering, counter resets, and
 * result construction. Implementations should document their behavior for empty input and any
 * metric fields that are not available in a sample.</p>
 *
 * @param <R> the projection result type
 * @author RollW
 */
@FunctionalInterface
public interface MetricReducer<R> {
    /**
     * Reduces samples from one metric series.
     *
     * <p>Callers must provide samples that share one metric series identity. Standard reducers
     * validate this requirement and order their input before calculating the result.</p>
     *
     * @param samples the raw samples from one metric series
     * @return the typed projection result
     */
    R reduce(Collection<? extends MetricSample> samples);
}
