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

/**
 * Receives immutable metric snapshots for application-owned storage or projection.
 *
 * <p>The sink does not own the supplied snapshot and must not mutate or retain a mutable view of
 * its contents. A sink may throw a {@link RuntimeException}; the component that owns delivery
 * decides how that failure is reported.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface MetricSnapshotSink {
    /**
     * Accepts one metric snapshot.
     *
     * @param snapshot the immutable snapshot to consume
     */
    void accept(MetricSnapshot snapshot);
}
