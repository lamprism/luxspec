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
 * A cache composed from ordered nearest-to-farthest levels.
 *
 * <p>A lower-level hit is promoted to missed nearer levels, and a loaded value is written through
 * every level. Mutations cover every level but are not atomic across independent providers. A
 * reported mutation failure means the levels may have diverged and the caller must apply its
 * provider-specific recovery policy.</p>
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @author RollW
 */
public interface MultiLevelCache<K, V> extends Cache<K, V> {
    /**
     * Composes at least two cache levels in nearest-to-farthest order.
     *
     * @param levels the independently configured cache levels
     * @param <K>    the cache key type
     * @param <V>    the cache value type
     * @return the composed cache
     */
    static <K, V> MultiLevelCache<K, V> of(List<? extends Cache<K, V>> levels) {
        return new OrderedMultiLevelCache<>(levels);
    }
}
