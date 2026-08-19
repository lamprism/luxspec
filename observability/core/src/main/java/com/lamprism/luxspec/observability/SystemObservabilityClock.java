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

package com.lamprism.luxspec.observability;

import java.time.Instant;

/**
 * Uses the JVM system clocks for observability measurements.
 *
 * @author RollW
 */
public class SystemObservabilityClock implements ObservabilityClock {
    @Override
    public long monotonicNanos() {
        return System.nanoTime();
    }

    @Override
    public Instant now() {
        return Instant.now();
    }
}
