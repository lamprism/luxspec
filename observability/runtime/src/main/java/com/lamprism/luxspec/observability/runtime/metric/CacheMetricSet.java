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

package com.lamprism.luxspec.observability.runtime.metric;

import com.lamprism.luxspec.cache.Cache;
import com.lamprism.luxspec.cache.CacheName;
import com.lamprism.luxspec.cache.CacheStatisticsSource;
import com.lamprism.luxspec.cache.CacheStats;
import com.lamprism.luxspec.observability.metric.FunctionCounterSpec;
import com.lamprism.luxspec.observability.metric.GaugeSpec;
import com.lamprism.luxspec.observability.metric.MetricCardinalityPolicy;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Exposes bounded operational statistics for explicitly owned caches.
 *
 * <p>Cache ownership remains with the component that created the cache. The set therefore accepts
 * an explicit name-to-cache map instead of trying to discover global caches. This keeps metric
 * registration deterministic and prevents unrelated caches from becoming observable by accident.</p>
 *
 * @author RollW
 */
public class CacheMetricSet implements MetricSet {
    private static final int MAXIMUM_CACHE_BINDINGS = 128;
    private final Map<CacheName, CacheStatisticsSource> sources;

    /**
     * Creates a metric set for one cache.
     *
     * @param name  the stable cache ownership name
     * @param cache the cache instance
     */
    public CacheMetricSet(CacheName name, Cache<?, ?> cache) {
        this(Map.of(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(cache, "cache")
        ));
    }

    /**
     * Creates a metric set for explicitly selected, statistics-enabled caches.
     *
     * @param caches the stable cache names and instances
     */
    public CacheMetricSet(Map<CacheName, ? extends Cache<?, ?>> caches) {
        Objects.requireNonNull(caches, "caches");
        Map<CacheName, CacheStatisticsSource> copied = new LinkedHashMap<>();
        for (Map.Entry<CacheName, ? extends Cache<?, ?>> entry : caches.entrySet()) {
            CacheName name = Objects.requireNonNull(entry.getKey(), "cache name");
            Cache<?, ?> cache = Objects.requireNonNull(entry.getValue(), "cache");
            if (!(cache instanceof CacheStatisticsSource source)) {
                throw new IllegalArgumentException(
                        "Cache does not expose statistics: " + name.getValue()
                );
            }
            if (copied.putIfAbsent(name, source) != null) {
                throw new IllegalArgumentException("Cache name is registered more than once: " + name);
            }
        }
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("At least one cache is required");
        }
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(copied));
    }

    /**
     * Starts building a selected cache metric set.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        MetricDimensionSpec<String> cacheDimension = MetricRegistrationSupport.dimension("cache");
        FunctionCounterSpec<Object> hits = counter(
                "cache.get.hit",
                "Successful cache lookups",
                source -> stats(source).getHitCount(),
                cacheDimension,
                "requests"
        );
        FunctionCounterSpec<Object> misses = counter(
                "cache.get.miss",
                "Unsuccessful cache lookups",
                source -> stats(source).getMissCount(),
                cacheDimension,
                "requests"
        );
        FunctionCounterSpec<Object> loads = counter(
                "cache.load.success",
                "Successful cache loads",
                source -> stats(source).getLoadSuccessCount(),
                cacheDimension,
                "loads"
        );
        FunctionCounterSpec<Object> loadFailures = counter(
                "cache.load.failure",
                "Failed cache loads",
                source -> stats(source).getLoadFailureCount(),
                cacheDimension,
                "loads"
        );
        FunctionCounterSpec<Object> loadDuration = counter(
                "cache.load.duration",
                "Cumulative cache load duration",
                source -> stats(source).getLoadDurationNanos(),
                cacheDimension,
                "nanoseconds"
        );
        FunctionCounterSpec<Object> evictions = counter(
                "cache.eviction",
                "Evicted cache entries",
                source -> stats(source).getEvictionCount(),
                cacheDimension,
                "entries"
        );
        GaugeSpec<Object> size = GaugeSpec
                .builder("cache.size", Object.class)
                .description("Current estimated cache size")
                .baseUnit("entries")
                .dimension(cacheDimension)
                .cardinality(MetricCardinalityPolicy.bounded(MAXIMUM_CACHE_BINDINGS))
                .reader(source -> stats(source).getCurrentSize())
                .build();

        for (Map.Entry<CacheName, CacheStatisticsSource> entry : sources.entrySet()) {
            String name = entry.getKey().getValue();
            Object cache = entry.getValue();
            MetricRegistrationSupport.register(nonNullRegistry, hits, cacheDimension, name, cache);
            MetricRegistrationSupport.register(nonNullRegistry, misses, cacheDimension, name, cache);
            MetricRegistrationSupport.register(nonNullRegistry, loads, cacheDimension, name, cache);
            MetricRegistrationSupport.register(nonNullRegistry, loadFailures, cacheDimension, name, cache);
            MetricRegistrationSupport.register(nonNullRegistry, loadDuration, cacheDimension, name, cache);
            MetricRegistrationSupport.register(nonNullRegistry, evictions, cacheDimension, name, cache);
            MetricRegistrationSupport.register(nonNullRegistry, size, cacheDimension, name, cache);
        }
    }

    private static FunctionCounterSpec<Object> counter(
            String name,
            String description,
            Function<Object, Number> reader,
            MetricDimensionSpec<String> dimension,
            String unit
    ) {
        return FunctionCounterSpec.builder(name, Object.class)
                .description(description)
                .baseUnit(unit)
                .dimension(dimension)
                .cardinality(MetricCardinalityPolicy.bounded(MAXIMUM_CACHE_BINDINGS))
                .reader(reader::apply)
                .build();
    }

    private static CacheStats stats(Object source) {
        CacheStatisticsSource statisticsSource = (CacheStatisticsSource) source;
        return statisticsSource.stats();
    }

    /**
     * Builds a cache metric set from explicit cache ownership entries.
     */
    public static class Builder {
        private final Map<CacheName, Cache<?, ?>> caches = new LinkedHashMap<>();

        public Builder cache(CacheName name, Cache<?, ?> cache) {
            CacheName nonNullName = Objects.requireNonNull(name, "name");
            Cache<?, ?> nonNullCache = Objects.requireNonNull(cache, "cache");
            if (caches.putIfAbsent(nonNullName, nonNullCache) != null) {
                throw new IllegalArgumentException("Cache name is registered more than once: " + name);
            }
            return this;
        }

        public CacheMetricSet build() {
            return new CacheMetricSet(caches);
        }
    }
}
