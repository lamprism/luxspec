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

/**
 * Optional operational-statistics capability for a cache implementation.
 *
 * <p>This role is intentionally separate from {@link Cache}. Applications that do not select
 * monitoring do not need to implement or invoke it.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface CacheStatisticsSource {
    /**
     * Reads one immutable statistics snapshot.
     *
     * @return the current statistics
     */
    CacheStats stats();
}
