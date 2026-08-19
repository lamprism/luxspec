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

import com.lamprism.luxspec.cache.CacheFactory;
import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.cache.CaffeineCacheFactory;
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
@ConditionalOnClass({CacheFactory.class, CaffeineCacheFactory.class})
@EnableConfigurationProperties(LuxspecCacheProperties.class)
public class LuxspecCacheAutoConfiguration {
    /**
     * Creates the default cache factory when an application has not supplied one.
     *
     * @return the Caffeine-backed cache factory
     */
    @Bean
    @ConditionalOnMissingBean(CacheFactory.class)
    public CacheFactory luxspecCacheFactory() {
        return new CaffeineCacheFactory();
    }

    /**
     * Creates the default generic cache profile from bound properties.
     *
     * @param properties the bound cache controls
     * @return the generic cache profile
     */
    @Bean
    @ConditionalOnMissingBean(CacheProfile.class)
    public CacheProfile luxspecCacheProfile(LuxspecCacheProperties properties) {
        return properties.toCacheProfile();
    }
}
