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

import java.util.List;

/**
 * Creates one logical cache from an explicit single-level or multi-level assembly plan.
 *
 * <p>A plan separates per-cache topology and retention controls from resource-owning cache
 * factories. Custom plans may implement this interface when their assembly semantics differ from
 * the built-in ordered plan.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface CachePlan {
    /**
     * Creates a single-level plan from one reusable factory and profile.
     *
     * @param factory the cache implementation factory
     * @param profile the profile for the created cache
     * @return the single-level cache plan
     */
    static CachePlan single(CacheFactory factory, CacheProfile profile) {
        return new DefaultCachePlan(List.of(CacheLevel.of(factory, profile)));
    }

    /**
     * Creates an ordered multi-level plan.
     *
     * <p>Every level receives the same logical cache name. Its factory must therefore address a
     * distinct storage tier rather than aliasing another level to the same backing entries.</p>
     *
     * @param levels at least two levels ordered nearest to farthest
     * @return the multi-level cache plan
     */
    static CachePlan multi(List<CacheLevel> levels) {
        return DefaultCachePlan.multi(levels);
    }

    /**
     * Creates one logical cache for the supplied ownership name.
     *
     * @param name the stable cache ownership name shared by every configured level
     * @param <K>  the cache key type
     * @param <V>  the cache value type
     * @return a single-level or multi-level cache according to this plan
     */
    <K, V> Cache<K, V> create(CacheName name);
}
