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

import com.lamprism.luxspec.observability.metric.MetricRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpServerMetricSetTest {
    @Test
    void registersHttpMetricDefinitions() {
        HttpServerMetricSet metricSet = new HttpServerMetricSet();

        try (MetricRegistry registry = MetricRegistryBuilder.builder().set(metricSet).build()) {
            assertEquals(2, registry.getSpecs().size());
            assertEquals(HttpServerMetricSet.REQUESTS, registry.find(
                    HttpServerMetricSet.REQUESTS.getName()
            ));
            assertEquals(HttpServerMetricSet.ACTIVE_REQUESTS, registry.find(
                    HttpServerMetricSet.ACTIVE_REQUESTS.getName()
            ));
        }
    }
}
