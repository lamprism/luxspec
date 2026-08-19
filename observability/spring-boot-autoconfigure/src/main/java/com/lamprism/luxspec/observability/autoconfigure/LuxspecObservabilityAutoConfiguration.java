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
import com.lamprism.luxspec.observability.runtime.ObservabilitySet;
import com.lamprism.luxspec.observability.runtime.health.HealthRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.health.JvmHealthSet;
import com.lamprism.luxspec.observability.runtime.metric.JvmMetricSet;
import com.lamprism.luxspec.observability.runtime.metric.MetricRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;

/**
 * Supplies explicit Luxspec observability registries and native health routes.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass(MetricRegistry.class)
@EnableConfigurationProperties(LuxspecObservabilitySettings.class)
public class LuxspecObservabilityAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(MetricRegistry.class)
    public MetricRegistry luxspecMetricRegistry(ObjectProvider<ObservabilitySet> sets) {
        MetricRegistryBuilder builder = MetricRegistryBuilder.builder();
        sets.orderedStream().forEach(builder::set);
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(ObservationRegistry.class)
    public ObservationRegistry luxspecObservationRegistry(ObjectProvider<ObservabilitySet> sets) {
        ObservationRegistryBuilder builder = ObservationRegistryBuilder.builder();
        sets.orderedStream().forEach(builder::set);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(HealthRegistry.class)
    @ConditionalOnSingleCandidate(Executor.class)
    public HealthRegistry luxspecHealthRegistry(
            Executor executor,
            ObjectProvider<ObservabilitySet> sets
    ) {
        HealthRegistryBuilder builder = HealthRegistryBuilder.builder(executor);
        sets.orderedStream().forEach(builder::set);
        return builder.build();
    }

    /**
     * Enables the selected, pull-based JVM metric domains.
     */
    @Bean
    @ConditionalOnProperty(prefix = "luxspec.observability.jvm.metrics", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(JvmMetricSet.class)
    public JvmMetricSet jvmMetricSet(LuxspecObservabilitySettings settings) {
        return JvmMetricSet.builder()
                .domains(settings.getJvm().getMetrics().getDomains())
                .build();
    }

    /**
     * Enables on-demand JVM liveness checks with configured thresholds.
     */
    @Bean
    @ConditionalOnProperty(prefix = "luxspec.observability.jvm.health", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(JvmHealthSet.class)
    public JvmHealthSet jvmHealthSet(LuxspecObservabilitySettings settings) {
        LuxspecObservabilitySettings.HealthSettings health = settings.getJvm().getHealth();
        return JvmHealthSet.builder()
                .memoryThreshold(health.getMemoryThreshold())
                .fileDescriptorThreshold(health.getFileDescriptorThreshold())
                .deadlockDetection(health.isDeadlockDetection())
                .build();
    }

    /**
     * Registers selected standard observation declarations without installing business
     * instrumentation.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "luxspec.observability.observation.standard",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean(StandardObservationSet.class)
    public StandardObservationSet standardObservationSet(LuxspecObservabilitySettings settings) {
        LuxspecObservabilitySettings.ObservationSettings observations = settings.getObservation();
        StandardObservationSet.Builder builder = StandardObservationSet.builder();
        observations.getStandard().getDomains().forEach(builder::domain);
        return builder.build();
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnBean(HealthRegistry.class)
    @ConditionalOnMissingBean(LuxspecHealthEndpoint.class)
    public LuxspecHealthEndpoint luxspecHealthEndpoint(HealthRegistry healthRegistry) {
        return new LuxspecHealthEndpoint(healthRegistry);
    }
}
