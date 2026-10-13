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

import com.lamprism.luxspec.observability.runtime.metric.JvmMetricSet;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationSet;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit selections for optional built-in observability sets.
 *
 * <p>All switches are disabled by default. Registry construction itself is cheap and inert; JVM
 * polling, health evaluation, HTTP request metrics, and standard operation declarations are only
 * assembled when a corresponding switch is enabled.</p>
 *
 * @author RollW
 */
@ConfigurationProperties("luxspec.observability")
public class LuxspecObservabilityConfiguration {
    private final JvmConfiguration jvm = new JvmConfiguration();
    private final HttpConfiguration http = new HttpConfiguration();
    private final ObservationConfiguration observation = new ObservationConfiguration();
    private final InstrumentationConfiguration instrumentation = new InstrumentationConfiguration();

    public JvmConfiguration getJvm() {
        return jvm;
    }

    public HttpConfiguration getHttp() {
        return http;
    }

    public ObservationConfiguration getObservation() {
        return observation;
    }

    public InstrumentationConfiguration getInstrumentation() {
        return instrumentation;
    }

    /**
     * JVM management selections.
     */
    public static class JvmConfiguration {
        private final MetricConfiguration metrics = new MetricConfiguration();
        private final HealthConfiguration health = new HealthConfiguration();

        public MetricConfiguration getMetrics() {
            return metrics;
        }

        public HealthConfiguration getHealth() {
            return health;
        }
    }

    /**
     * HTTP server metric selections.
     */
    public static class HttpConfiguration {
        private final HttpMetricConfiguration metrics = new HttpMetricConfiguration();
        private final HttpObservationConfiguration observation = new HttpObservationConfiguration();

        public HttpMetricConfiguration getMetrics() {
            return metrics;
        }

        public HttpObservationConfiguration getObservation() {
            return observation;
        }
    }

    /**
     * HTTP server observation selection.
     */
    public static class HttpObservationConfiguration {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * HTTP server metric selection.
     */
    public static class HttpMetricConfiguration {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * JVM metric selection.
     */
    public static class MetricConfiguration {
        private boolean enabled;
        private Set<JvmMetricSet.Domain> domains = EnumSet.allOf(JvmMetricSet.Domain.class);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Set<JvmMetricSet.Domain> getDomains() {
            return domains;
        }

        public void setDomains(Set<JvmMetricSet.Domain> domains) {
            Set<JvmMetricSet.Domain> selected = Objects.requireNonNull(domains, "domains");
            this.domains = selected.isEmpty()
                    ? EnumSet.noneOf(JvmMetricSet.Domain.class)
                    : EnumSet.copyOf(selected);
        }
    }

    /**
     * JVM health selection and thresholds.
     */
    public static class HealthConfiguration {
        private boolean enabled;
        private double memoryThreshold = 0.95d;
        private double fileDescriptorThreshold = 0.95d;
        private boolean deadlockDetection = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getMemoryThreshold() {
            return memoryThreshold;
        }

        public void setMemoryThreshold(double memoryThreshold) {
            this.memoryThreshold = memoryThreshold;
        }

        public double getFileDescriptorThreshold() {
            return fileDescriptorThreshold;
        }

        public void setFileDescriptorThreshold(double fileDescriptorThreshold) {
            this.fileDescriptorThreshold = fileDescriptorThreshold;
        }

        public boolean isDeadlockDetection() {
            return deadlockDetection;
        }

        public void setDeadlockDetection(boolean deadlockDetection) {
            this.deadlockDetection = deadlockDetection;
        }
    }

    /**
     * Standard operation observation selection.
     */
    public static class ObservationConfiguration {
        private final StandardConfiguration standard = new StandardConfiguration();

        public StandardConfiguration getStandard() {
            return standard;
        }
    }

    /**
     * Explicit infrastructure bean selections for observation wrappers.
     *
     * <p>Empty selections keep automatic wrapping disabled. Bean names are used instead of broad
     * type matching because replacing a framework-owned implementation can change its public
     * concrete type, lifecycle, or delegate unwrapping behavior.</p>
     */
    public static class InstrumentationConfiguration {
        private Set<String> dataSourceBeans = Set.of();
        private Set<String> executorBeans = Set.of();
        private Set<String> queryExecutorBeans = Set.of();

        public Set<String> getDataSourceBeans() {
            return dataSourceBeans;
        }

        public void setDataSourceBeans(Set<String> dataSourceBeans) {
            this.dataSourceBeans = copyBeanNames(dataSourceBeans, "dataSourceBeans");
        }

        public Set<String> getExecutorBeans() {
            return executorBeans;
        }

        public void setExecutorBeans(Set<String> executorBeans) {
            this.executorBeans = copyBeanNames(executorBeans, "executorBeans");
        }

        public Set<String> getQueryExecutorBeans() {
            return queryExecutorBeans;
        }

        public void setQueryExecutorBeans(Set<String> queryExecutorBeans) {
            this.queryExecutorBeans = copyBeanNames(queryExecutorBeans, "queryExecutorBeans");
        }

        private static Set<String> copyBeanNames(Set<String> names, String propertyName) {
            Set<String> selected = new LinkedHashSet<>();
            for (String name : Objects.requireNonNull(names, propertyName)) {
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException(propertyName + " must not contain blank names");
                }
                selected.add(name);
            }
            return Set.copyOf(selected);
        }
    }

    /**
     * Standard operation observation selection.
     */
    public static class StandardConfiguration {
        private boolean enabled;
        private Set<StandardObservationSet.Domain> domains =
                EnumSet.allOf(StandardObservationSet.Domain.class);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Set<StandardObservationSet.Domain> getDomains() {
            return domains;
        }

        public void setDomains(Set<StandardObservationSet.Domain> domains) {
            Set<StandardObservationSet.Domain> selected = Objects.requireNonNull(domains, "domains");
            this.domains = selected.isEmpty()
                    ? EnumSet.noneOf(StandardObservationSet.Domain.class)
                    : EnumSet.copyOf(selected);
        }
    }
}
