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

package com.lamprism.luxspec.observability.autoconfigure;

import com.lamprism.luxspec.observability.health.HealthRegistry;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.runtime.health.HealthRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.metric.MetricRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;

/**
 * Supplies explicit Luxspec observability registries and native health routes.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass(MetricRegistry.class)
public class LuxspecObservabilityAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(MetricRegistry.class)
    public MetricRegistry luxspecMetricRegistry() {
        return MetricRegistryBuilder.builder().build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(ObservationRegistry.class)
    public ObservationRegistry luxspecObservationRegistry() {
        return ObservationRegistryBuilder.builder().build();
    }

    @Bean
    @ConditionalOnMissingBean(HealthRegistry.class)
    @ConditionalOnSingleCandidate(Executor.class)
    public HealthRegistry luxspecHealthRegistry(Executor executor) {
        return HealthRegistryBuilder.builder(executor).build();
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnBean(HealthRegistry.class)
    @ConditionalOnMissingBean(LuxspecHealthEndpoint.class)
    public LuxspecHealthEndpoint luxspecHealthEndpoint(HealthRegistry healthRegistry) {
        return new LuxspecHealthEndpoint(healthRegistry);
    }
}
