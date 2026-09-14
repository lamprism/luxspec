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

import com.lamprism.luxspec.observability.metric.DistributionSpec;
import com.lamprism.luxspec.observability.metric.GaugeSpec;
import com.lamprism.luxspec.observability.metric.MetricCardinalityPolicy;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.TimerSpec;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Declares bounded inbound HTTP server metrics for an HTTP adapter.
 *
 * <p>The set contains metric definitions only. Adapters own request lifecycle, input normalization,
 * and binding updates. Route values must be framework-provided templates rather than raw request
 * URIs.</p>
 *
 * @author RollW
 */
public class HttpServerMetricSet implements MetricSet {
    public static final String UNKNOWN = "UNKNOWN";
    public static final int MAXIMUM_METHOD_LENGTH = 16;
    public static final int MAXIMUM_ROUTE_LENGTH = 256;

    public static final MetricDimensionSpec<String> METHOD = MetricDimensionSpec
            .string("method")
            .maxValueLength(MAXIMUM_METHOD_LENGTH)
            .build();
    public static final MetricDimensionSpec<String> ROUTE = MetricDimensionSpec
            .string("route")
            .maxValueLength(MAXIMUM_ROUTE_LENGTH)
            .build();
    public static final MetricDimensionSpec<String> STATUS = MetricDimensionSpec
            .string("status")
            .maxValueLength(7)
            .build();
    public static final MetricDimensionSpec<String> OUTCOME = MetricDimensionSpec.allowed(
            "outcome",
            "INFORMATIONAL",
            "SUCCESS",
            "REDIRECTION",
            "CLIENT_ERROR",
            "SERVER_ERROR",
            UNKNOWN
    );

    private static final int MAXIMUM_REQUEST_BINDINGS = 2048;
    public static final TimerSpec REQUESTS = TimerSpec.builder("http.server.requests")
            .description("Inbound HTTP server request duration")
            .baseUnit("seconds")
            .dimension(METHOD)
            .dimension(ROUTE)
            .dimension(STATUS)
            .dimension(OUTCOME)
            .cardinality(MetricCardinalityPolicy.bounded(MAXIMUM_REQUEST_BINDINGS))
            .distribution(requestDistribution())
            .build();
    public static final GaugeSpec<LongSupplier> ACTIVE_REQUESTS = GaugeSpec
            .builder("http.server.active.requests", LongSupplier.class)
            .description("Active inbound HTTP server requests")
            .baseUnit("requests")
            .reader(source -> source.getAsLong())
            .build();

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        nonNullRegistry.register(REQUESTS);
        nonNullRegistry.register(ACTIVE_REQUESTS);
    }

    private static DistributionSpec requestDistribution() {
        return DistributionSpec.builder()
                .serviceLevelObjectives(
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1),
                        Duration.ofMillis(2500),
                        Duration.ofSeconds(5)
                )
                .build();
    }
}
