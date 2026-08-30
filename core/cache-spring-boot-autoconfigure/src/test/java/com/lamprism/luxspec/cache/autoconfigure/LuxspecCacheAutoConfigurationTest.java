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

package com.lamprism.luxspec.cache.autoconfigure;

import com.lamprism.luxspec.cache.Cache;
import com.lamprism.luxspec.cache.CacheFactory;
import com.lamprism.luxspec.cache.CacheName;
import com.lamprism.luxspec.cache.CachePlan;
import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.cache.CacheStatisticsSource;
import com.lamprism.luxspec.cache.CaffeineCacheFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecCacheAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecCacheAutoConfiguration.class));

    @Test
    void createsDefaultCachePlanFromTheFactoryAndBoundProfile() {
        contextRunner
                .withPropertyValues(
                        "luxspec.cache.maximum-size=32",
                        "luxspec.cache.expire-after-write=10s",
                        "luxspec.cache.record-stats=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheFactory.class);
                    assertThat(context).hasSingleBean(CachePlan.class);
                    assertThat(context).hasSingleBean(CacheProfile.class);
                    assertThat(context).doesNotHaveBean(Cache.class);
                    LuxspecCacheProperties properties = context.getBean(LuxspecCacheProperties.class);
                    assertThat(properties.getMaximumSize()).isEqualTo(32);
                    assertThat(properties.getExpireAfterWrite()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(properties.isRecordStats()).isTrue();
                    CacheProfile profile = context.getBean(CacheProfile.class);
                    assertThat(profile.getMaximumSize()).isEqualTo(32);
                    assertThat(profile.getExpireAfterWrite()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(profile.isRecordStats()).isTrue();
                    Cache<?, ?> cache = context.getBean(CachePlan.class)
                            .create(CacheName.of("test"));
                    assertThat(cache).isInstanceOf(CacheStatisticsSource.class);
                });
    }

    @Test
    void keepsTheDefaultFactoryAsFallbackWhenApplicationProvidesOne() {
        CacheFactory applicationFactory = new CaffeineCacheFactory();

        contextRunner
                .withBean(CacheFactory.class, () -> applicationFactory)
                .run(context -> {
                    assertThat(context.getBean(CacheFactory.class)).isSameAs(applicationFactory);
                    assertThat(context.getBeansOfType(CacheFactory.class)).hasSize(2);
                    assertThat(context).doesNotHaveBean(Cache.class);
                });
    }

    @Test
    void keepsTheDefaultProfileAsFallbackWhenApplicationProvidesOne() {
        CacheProfile applicationProfile = CacheProfile.builder().maximumSize(8).build();

        contextRunner
                .withBean(CacheProfile.class, () -> applicationProfile)
                .run(context -> {
                    assertThat(context.getBean(CacheProfile.class)).isSameAs(applicationProfile);
                    assertThat(context.getBeansOfType(CacheProfile.class)).hasSize(2);
                });
    }

    @Test
    void keepsTheDefaultPlanAsFallbackWhenApplicationProvidesOne() {
        CachePlan applicationPlan = CachePlan.single(
                new CaffeineCacheFactory(),
                CacheProfile.defaults()
        );

        contextRunner
                .withBean(CachePlan.class, () -> applicationPlan)
                .run(context -> {
                    assertThat(context.getBean(CachePlan.class)).isSameAs(applicationPlan);
                    assertThat(context.getBeansOfType(CachePlan.class)).hasSize(2);
                });
    }

}
