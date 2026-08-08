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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Supplies the default bounded Caffeine implementation of the generic cache contract.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass({Cache.class, CaffeineCaches.class})
@EnableConfigurationProperties(LuxspecCacheProperties.class)
public class LuxspecCacheAutoConfiguration {
    /**
     * Creates the default cache when an application has not supplied one.
     *
     * @param properties the bound cache controls
     * @return the Caffeine-backed generic cache
     */
    @Bean
    @ConditionalOnMissingBean(Cache.class)
    public Cache<Object, Object> luxspecCache(LuxspecCacheProperties properties) {
        return CaffeineCaches.create(properties.toCacheProfile());
    }
}
