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

package com.lamprism.luxspec.observability.micrometer;

import com.lamprism.luxspec.observability.observation.ObservationLink;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.observation.ObservationStart;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicrometerObservationBridgeTest {
    @Test
    void preservesProviderNeutralLinksInTheProviderContext() {
        ObservationLink link = ObservationLink.of("trace-1", "span-1");
        List<ObservationLink> observedLinks = new ArrayList<>();
        io.micrometer.observation.ObservationRegistry providerRegistry =
                io.micrometer.observation.ObservationRegistry.create();
        providerRegistry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override
            public void onStart(Observation.Context context) {
                observedLinks.addAll(MicrometerObservationLinkContext.get(context));
            }

            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        });
        ObservationSpec spec = ObservationSpec.builder("test.operation").build();

        try (var registry = ObservationRegistryBuilder.builder().build();
             MicrometerObservationBridge ignored = new MicrometerObservationBridge(registry, providerRegistry)) {
            registry.register(spec);
            registry.start(ObservationStart.builder(spec).link(link).build()).stop();
        }

        assertEquals(List.of(link), observedLinks);
    }
}
