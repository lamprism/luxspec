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

import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationView;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationSet;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuxspecHttpObservationFilterTest {
    @Test
    void recordsTheStandardHttpObservationEnvelope() throws Exception {
        AtomicReference<ObservationView> stopped = new AtomicReference<>();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStop(ObservationView view) {
                stopped.set(view);
            }
        };
        try (ObservationRegistry registry = ObservationRegistryBuilder.builder()
                .set(StandardObservationSet.builder().domain(StandardObservationSet.Domain.WEB).build())
                .handler(handler)
                .build()) {
            LuxspecHttpObservationFilter filter = new LuxspecHttpObservationFilter(registry);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts/42");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/accounts/{id}");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (servletRequest, servletResponse) ->
                    ((MockHttpServletResponse) servletResponse).setStatus(201));

            ObservationView view = stopped.get();
            assertEquals(ObservationOutcome.SUCCESS, view.outcome());
            assertEquals("GET", view.attributes().get(LuxspecHttpObservationFilter.METHOD).orElseThrow());
            assertEquals("/accounts/{id}", view.attributes().get(LuxspecHttpObservationFilter.ROUTE).orElseThrow());
            assertEquals("201", view.attributes().get(LuxspecHttpObservationFilter.STATUS).orElseThrow());
        }
    }
}
