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

import com.lamprism.luxspec.cache.Cache;
import com.lamprism.luxspec.cache.CacheFactory;
import com.lamprism.luxspec.cache.CacheInvalidator;
import com.lamprism.luxspec.cache.CacheName;
import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigValue;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Caches resolved configuration values and invalidates every definition for one complete key.
 *
 * @author RollW
 */
public class ConfigValueCache implements CacheInvalidator<ConfigKey> {
    private static final CacheName CACHE_NAME = CacheName.of("config.values");
    private final Cache<ConfigCacheKey, ConfigValue<?>> cache;

    /**
     * Creates a configuration-owned value cache through the supplied factory.
     *
     * @param factory the cache instance factory
     * @param profile the generic cache retention profile
     */
    public ConfigValueCache(CacheFactory factory, CacheProfile profile) {
        CacheFactory nonNullFactory = Objects.requireNonNull(factory, "factory");
        CacheProfile nonNullProfile = Objects.requireNonNull(profile, "profile");
        this.cache = nonNullFactory.create(CACHE_NAME, nonNullProfile);
    }

    /**
     * Returns a cached value or loads one through the supplied resolver.
     *
     * @param binding the resolved configuration definition and complete key
     * @param loader  the uncached value resolver
     * @param <T>     the configuration value type
     * @return the cached or loaded value
     */
    public <T> ConfigValue<T> get(
            ConfigBinding<T> binding,
            Supplier<? extends ConfigValue<T>> loader
    ) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        Supplier<? extends ConfigValue<T>> nonNullLoader = Objects.requireNonNull(loader, "loader");
        ConfigCacheKey cacheKey = ConfigCacheKey.of(nonNullBinding);
        ConfigValue<?> value = cache.get(
                cacheKey,
                ignored -> Objects.requireNonNull(nonNullLoader.get(), "loaded configuration value")
        );
        return cast(value);
    }

    /**
     * Invalidates every cached configuration definition bound to one complete key.
     *
     * @param key the complete configuration key
     */
    @Override
    public void invalidate(ConfigKey key) {
        ConfigKey nonNullKey = Objects.requireNonNull(key, "key");
        cache.invalidateAll(cacheKey -> nonNullKey.equals(cacheKey.getKey()));
    }

    @SuppressWarnings("unchecked")
    private static <T> ConfigValue<T> cast(ConfigValue<?> value) {
        return (ConfigValue<T>) value;
    }
}
