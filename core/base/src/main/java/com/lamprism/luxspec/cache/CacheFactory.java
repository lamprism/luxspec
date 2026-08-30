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
 * Creates independently owned typed cache instances through one cache implementation.
 *
 * <p>The factory owns reusable implementation resources such as clients, serialization, and remote
 * access. The creation boundary supplies the stable ownership name and provider-neutral profile for
 * each cache instance. Logical single-level and multi-level assembly belongs to {@link CachePlan}.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface CacheFactory {
    /**
     * Creates one cache instance for an ownership boundary and profile.
     *
     * @param name    the stable cache ownership name
     * @param profile the provider-neutral cache profile
     * @param <K>     the cache key type
     * @param <V>     the cache value type
     * @return a new cache instance
     */
    <K, V> Cache<K, V> create(CacheName name, CacheProfile profile);
}
