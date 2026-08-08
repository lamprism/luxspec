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
import com.lamprism.luxspec.cache.CaffeineCaches;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecCacheAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecCacheAutoConfiguration.class));

    @Test
    void createsDefaultCacheFromBoundProperties() {
        contextRunner
                .withPropertyValues(
                        "luxspec.cache.maximum-size=32",
                        "luxspec.cache.expire-after-write=10s"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(Cache.class);
                    LuxspecCacheProperties properties = context.getBean(LuxspecCacheProperties.class);
                    assertThat(properties.getMaximumSize()).isEqualTo(32);
                    assertThat(properties.getExpireAfterWrite()).isEqualTo(Duration.ofSeconds(10));
                });
    }

    @Test
    void backsOffWhenApplicationProvidesCache() {
        Cache<Object, Object> applicationCache = CaffeineCaches.create();

        contextRunner
                .withBean(Cache.class, () -> applicationCache)
                .run(context -> assertThat(context.getBean(Cache.class)).isSameAs(applicationCache));
    }
}
