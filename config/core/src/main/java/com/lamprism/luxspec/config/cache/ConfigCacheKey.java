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

package com.lamprism.luxspec.config.cache;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;

import java.util.Objects;

/**
 * Identifies one typed configuration cache entry by Spec identity and complete key.
 *
 * @author RollW
 */
public final class ConfigCacheKey {
    private final ConfigSpec<?> spec;
    private final ConfigKey key;

    private ConfigCacheKey(ConfigSpec<?> spec, ConfigKey key) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.key = Objects.requireNonNull(key, "key");
    }

    /**
     * Creates a cache key from one binding.
     *
     * @param binding the complete typed binding
     * @return the cache key
     */
    public static ConfigCacheKey of(ConfigBinding<?> binding) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        return new ConfigCacheKey(nonNullBinding.getSpec(), nonNullBinding.getKey());
    }

    /**
     * Returns the owning definition identity.
     *
     * @return the owning Spec
     */
    public ConfigSpec<?> getSpec() {
        return spec;
    }

    /**
     * Returns the complete key.
     *
     * @return the complete key
     */
    public ConfigKey getKey() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigCacheKey cacheKey)) {
            return false;
        }
        return spec == cacheKey.spec && key.equals(cacheKey.key);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(spec) + key.hashCode();
    }
}
