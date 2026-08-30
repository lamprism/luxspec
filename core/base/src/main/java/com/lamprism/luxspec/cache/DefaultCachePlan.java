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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class DefaultCachePlan implements CachePlan {
    private final List<CacheLevel> levels;

    DefaultCachePlan(List<CacheLevel> levels) {
        this.levels = copyLevels(levels);
    }

    static CachePlan multi(List<CacheLevel> levels) {
        List<CacheLevel> copied = copyLevels(levels);
        if (copied.size() < 2) {
            throw new IllegalArgumentException("A multi-level cache plan requires at least two levels");
        }
        return new DefaultCachePlan(copied);
    }

    @Override
    public <K, V> Cache<K, V> create(CacheName name) {
        CacheName nonNullName = Objects.requireNonNull(name, "name");
        if (levels.size() == 1) {
            return createLevel(levels.get(0), nonNullName);
        }
        List<Cache<K, V>> caches = new ArrayList<>(levels.size());
        for (CacheLevel level : levels) {
            caches.add(createLevel(level, nonNullName));
        }
        return MultiLevelCache.of(caches);
    }

    private static List<CacheLevel> copyLevels(List<CacheLevel> levels) {
        List<CacheLevel> nonNullLevels = Objects.requireNonNull(levels, "levels");
        if (nonNullLevels.isEmpty()) {
            throw new IllegalArgumentException("A cache plan requires at least one level");
        }
        List<CacheLevel> copied = new ArrayList<>(nonNullLevels.size());
        for (CacheLevel level : nonNullLevels) {
            copied.add(Objects.requireNonNull(level, "level"));
        }
        return List.copyOf(copied);
    }

    private static <K, V> Cache<K, V> createLevel(CacheLevel level, CacheName name) {
        return level.factory().create(name, level.profile());
    }
}
