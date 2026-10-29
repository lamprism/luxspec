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
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author RollW
 */
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

    @Test
    void appliesTheSelectedOutcomePolicyWhenAsyncProcessingCompletes() throws Exception {
        AtomicReference<ObservationView> stopped = new AtomicReference<>();
        AtomicInteger stopCount = new AtomicInteger();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStop(ObservationView view) {
                stopped.set(view);
                stopCount.incrementAndGet();
            }
        };
        try (ObservationRegistry registry = ObservationRegistryBuilder.builder()
                .set(StandardObservationSet.builder().domain(StandardObservationSet.Domain.WEB).build())
                .handler(handler)
                .build()) {
            LuxspecHttpObservationFilter filter = new LuxspecHttpObservationFilter(
                    registry, HttpObservationOutcomePolicy.clientAndServerErrors()
            );
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/async");
            request.setAsyncSupported(true);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (servletRequest, servletResponse) ->
                    servletRequest.startAsync(servletRequest, servletResponse));
            assertNull(stopped.get());

            MockAsyncContext async = (MockAsyncContext) request.getAsyncContext();
            AsyncListener listener = async.getListeners().get(0);
            listener.onStartAsync(new AsyncEvent(async));
            response.setStatus(404);
            async.complete();

            assertEquals(1, stopCount.get());
            assertEquals(ObservationOutcome.ERROR, stopped.get().outcome());
            assertEquals("404", stopped.get().attributes().get(LuxspecHttpObservationFilter.STATUS).orElseThrow());
        }
    }

    @Test
    void stopsOnlyOnceWhenAnAsyncTimeoutIsFollowedByCompletion() throws Exception {
        AtomicReference<ObservationView> stopped = new AtomicReference<>();
        AtomicInteger stopCount = new AtomicInteger();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStop(ObservationView view) {
                stopped.set(view);
                stopCount.incrementAndGet();
            }
        };
        try (ObservationRegistry registry = ObservationRegistryBuilder.builder()
                .set(StandardObservationSet.builder().domain(StandardObservationSet.Domain.WEB).build())
                .handler(handler)
                .build()) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/async");
            request.setAsyncSupported(true);
            MockHttpServletResponse response = new MockHttpServletResponse();
            new LuxspecHttpObservationFilter(registry).doFilter(request, response, (servletRequest, servletResponse) ->
                    servletRequest.startAsync(servletRequest, servletResponse));
            MockAsyncContext async = (MockAsyncContext) request.getAsyncContext();

            async.getListeners().get(0).onTimeout(new AsyncEvent(async));
            async.complete();

            assertEquals(1, stopCount.get());
            assertEquals(ObservationOutcome.ERROR, stopped.get().outcome());
            assertEquals("500", stopped.get().attributes().get(LuxspecHttpObservationFilter.STATUS).orElseThrow());
        }
    }
}
