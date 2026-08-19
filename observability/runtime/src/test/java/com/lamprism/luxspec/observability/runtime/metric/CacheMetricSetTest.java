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
import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.cache.CaffeineCacheFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheMetricSetTest {
    @Test
    void requiresExplicitCacheStatistics() {
        Cache<String, String> cache = new CaffeineCacheFactory().create(
                CacheName.of("plain"),
                CacheProfile.defaults()
        );

        assertThrows(IllegalArgumentException.class, () -> new CacheMetricSet(CacheName.of("plain"), cache));
    }

    @Test
    void registersMetricsForAnExplicitStatisticsEnabledCache() {
        Cache<String, String> cache = new CaffeineCacheFactory().create(
                CacheName.of("observed"),
                CacheProfile.builder().recordStats(true).build()
        );
        cache.get("entry", key -> "value");

        try (var registry = MetricRegistryBuilder.builder()
                .set(new CacheMetricSet(CacheName.of("observed"), cache))
                .build()) {
            assertEquals(7, registry.getSpecs().size());
            assertEquals(1L, registry.snapshot().readings().stream()
                    .filter(reading -> reading.binding().getSpec().getName().getValue()
                            .equals("cache.load.success"))
                    .findFirst()
                    .orElseThrow()
                    .value()
                    .longValue());
        }
    }
}
