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
import com.lamprism.luxspec.observability.runtime.health.JvmHealthSet;
import com.lamprism.luxspec.observability.runtime.metric.HttpServerMetricSet;
import com.lamprism.luxspec.observability.runtime.metric.JvmMetricSet;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationSet;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecObservabilityAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecObservabilityAutoConfiguration.class));
    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecObservabilityAutoConfiguration.class));

    @Test
    void createsDefaultRegistriesAndHealthWhenOneExecutorIsAvailable() {
        contextRunner
                .withBean(Executor.class, () -> Runnable::run)
                .run(context -> {
                    assertThat(context).hasSingleBean(MetricRegistry.class);
                    assertThat(context).hasSingleBean(ObservationRegistry.class);
                    assertThat(context).hasSingleBean(HealthRegistry.class);
                });
    }

    @Test
    void backsOffApplicationOwnedRegistries() {
        MetricRegistry metricRegistry = new StubMetricRegistry();
        ObservationRegistry observationRegistry = new StubObservationRegistry();
        contextRunner
                .withBean(MetricRegistry.class, () -> metricRegistry)
                .withBean(ObservationRegistry.class, () -> observationRegistry)
                .run(context -> {
                    assertThat(context.getBean(MetricRegistry.class)).isSameAs(metricRegistry);
                    assertThat(context.getBean(ObservationRegistry.class)).isSameAs(observationRegistry);
                });
    }

    @Test
    void keepsOptionalObservabilitySetsDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(JvmMetricSet.class);
            assertThat(context).doesNotHaveBean(JvmHealthSet.class);
            assertThat(context).doesNotHaveBean(StandardObservationSet.class);
        });
    }

    @Test
    void keepsHttpMetricsDisabledByDefault() {
        webContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(HttpServerMetricSet.class);
            assertThat(context).doesNotHaveBean(LuxspecHttpMetricsFilter.class);
        });
    }

    @Test
    void doesNotEnableHttpMetricsForNonServletApplications() {
        contextRunner
                .withPropertyValues("luxspec.observability.http.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(HttpServerMetricSet.class);
                    assertThat(context).doesNotHaveBean(LuxspecHttpMetricsFilter.class);
                });
    }

    @Test
    void enablesHttpMetricsForServletApplications() {
        webContextRunner
                .withPropertyValues("luxspec.observability.http.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpServerMetricSet.class);
                    assertThat(context).hasSingleBean(LuxspecHttpMetricsFilter.class);
                    assertThat(context.getBean(MetricRegistry.class).getSpecs())
                            .extracting(spec -> spec.getName().getValue())
                            .contains("http.server.requests", "http.server.active.requests");
                });
    }

    @Test
    void assemblesSelectedJvmAndStandardSetsThroughOneRegistryBoundary() {
        contextRunner
                .withPropertyValues(
                        "luxspec.observability.jvm.metrics.enabled=true",
                        "luxspec.observability.jvm.metrics.domains=THREAD",
                        "luxspec.observability.jvm.health.enabled=true",
                        "luxspec.observability.observation.standard.enabled=true",
                        "luxspec.observability.observation.standard.domains=WEB,SECURITY"
                )
                .withBean(Executor.class, () -> Runnable::run)
                .run(context -> {
                    assertThat(context).hasSingleBean(JvmMetricSet.class);
                    assertThat(context).hasSingleBean(JvmHealthSet.class);
                    assertThat(context).hasSingleBean(StandardObservationSet.class);
                    assertThat(context.getBean(MetricRegistry.class).getSpecs())
                            .extracting(spec -> spec.getName().getValue())
                            .contains("jvm.threads.live");
                    assertThat(context.getBean(ObservationRegistry.class).getSpecs())
                            .extracting(spec -> spec.getName().value())
                            .contains("web.request", "security.authentication");
                });
    }

    private static final class StubMetricRegistry implements MetricRegistry {
        @Override
        public void register(com.lamprism.luxspec.observability.metric.MetricSpec<?> spec) {
        }

        @Override
        public com.lamprism.luxspec.observability.metric.MetricSpec<?> find(
                com.lamprism.luxspec.observability.metric.MetricName name
        ) {
            return null;
        }

        @Override
        public <M extends com.lamprism.luxspec.observability.metric.Metric> M obtain(
                com.lamprism.luxspec.observability.metric.MetricBinding<M> binding
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <M extends com.lamprism.luxspec.observability.metric.Metric> M obtain(
                com.lamprism.luxspec.observability.metric.MetricSpec<M> spec
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <M extends com.lamprism.luxspec.observability.metric.Metric> M find(
                com.lamprism.luxspec.observability.metric.MetricBinding<M> binding
        ) {
            return null;
        }

        @Override
        public java.util.List<com.lamprism.luxspec.observability.metric.MetricSpec<?>> getSpecs() {
            return java.util.List.of();
        }

        @Override
        public com.lamprism.luxspec.observability.metric.MetricSnapshot snapshot() {
            return new com.lamprism.luxspec.observability.metric.MetricSnapshot(
                    java.time.Instant.EPOCH,
                    0L,
                    java.util.List.of()
            );
        }

        @Override
        public void close() {
        }
    }

    private static final class StubObservationRegistry implements ObservationRegistry {
        @Override
        public void register(com.lamprism.luxspec.observability.observation.@NonNull ObservationSpec spec) {
        }

        @Override
        public com.lamprism.luxspec.observability.observation.ObservationSpec find(
                com.lamprism.luxspec.observability.observation.ObservationName name
        ) {
            return null;
        }

        @Override
        public com.lamprism.luxspec.observability.observation.Observation start(
                com.lamprism.luxspec.observability.observation.@NonNull ObservationSpec spec
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.lamprism.luxspec.observability.observation.Observation start(
                com.lamprism.luxspec.observability.observation.@NonNull ObservationStart start
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<com.lamprism.luxspec.observability.observation.ObservationSpec> getSpecs() {
            return java.util.List.of();
        }

        @Override
        public void close() {
        }
    }
}
