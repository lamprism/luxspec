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

import java.util.Objects;

/**
 * Defines one logical cache level through a reusable factory and its per-level profile.
 *
 * <p>A factory may be reused across plans and logical cache names while each level retains its own
 * profile. Within one multi-level plan, factories must create distinct storage tiers for the shared
 * logical cache name.</p>
 *
 * @param factory the cache implementation factory
 * @param profile the profile for this cache level
 * @author RollW
 */
public record CacheLevel(CacheFactory factory, CacheProfile profile) {
    /**
     * Creates one validated cache level definition.
     *
     * @param factory the cache implementation factory
     * @param profile the profile for this cache level
     */
    public CacheLevel {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(profile, "profile");
    }

    /**
     * Creates one cache level definition.
     *
     * @param factory the cache implementation factory
     * @param profile the profile for this cache level
     * @return the immutable cache level definition
     */
    public static CacheLevel of(CacheFactory factory, CacheProfile profile) {
        return new CacheLevel(factory, profile);
    }
}
