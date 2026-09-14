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

import com.lamprism.luxspec.observability.metric.MetricReading;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import com.lamprism.luxspec.observability.runtime.metric.HttpServerMetricSet;
import com.lamprism.luxspec.observability.runtime.metric.MetricRegistryBuilder;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LuxspecHttpMetricsFilterTest {
    @Test
    void recordsTemplateStatusOutcomeAndActiveRequests() throws Exception {
        HttpServerMetricSet metricSet = new HttpServerMetricSet();
        try (MetricRegistry registry = MetricRegistryBuilder.builder().set(metricSet).build()) {
            LuxspecHttpMetricsFilter filter = new LuxspecHttpMetricsFilter(registry);
            MockHttpServletRequest request = new MockHttpServletRequest("get", "/accounts/42");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/accounts/{id}");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                assertEquals(1.0d, reading(registry.snapshot(), "http.server.active.requests").value());
                ((MockHttpServletResponse) servletResponse).setStatus(201);
            });

            MetricReading reading = reading(registry.snapshot(), "http.server.requests");
            assertEquals(
                    Map.of(
                            "method", "GET",
                            "route", "/accounts/{id}",
                            "status", "201",
                            "outcome", "SUCCESS"
                    ),
                    reading.binding().getDimensions()
            );
            assertEquals(1L, reading.count());
            assertEquals(0.0d, reading(registry.snapshot(), "http.server.active.requests").value());
        }
    }

    @Test
    void recordsUnhandledExceptionAsServerErrorAndRethrows() throws Exception {
        HttpServerMetricSet metricSet = new HttpServerMetricSet();
        try (MetricRegistry registry = MetricRegistryBuilder.builder().set(metricSet).build()) {
            LuxspecHttpMetricsFilter filter = new LuxspecHttpMetricsFilter(registry);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/failure");
            MockHttpServletResponse response = new MockHttpServletResponse();
            ServletException failure = new ServletException("expected");

            ServletException thrown = assertThrows(
                    ServletException.class,
                    () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                        throw failure;
                    })
            );

            assertSame(failure, thrown);
            assertEquals(
                    Map.of(
                            "method", "GET",
                            "route", "UNKNOWN",
                            "status", "500",
                            "outcome", "SERVER_ERROR"
                    ),
                    reading(registry.snapshot(), "http.server.requests").binding().getDimensions()
            );
            assertEquals(0.0d, reading(registry.snapshot(), "http.server.active.requests").value());
        }
    }

    @Test
    void recordsAnAsyncRequestWhenItsContextCompletes() throws Exception {
        HttpServerMetricSet metricSet = new HttpServerMetricSet();
        try (MetricRegistry registry = MetricRegistryBuilder.builder().set(metricSet).build()) {
            LuxspecHttpMetricsFilter filter = new LuxspecHttpMetricsFilter(registry);
            CompletingAsyncRequest request = new CompletingAsyncRequest("GET", "/async");
            request.setAsyncSupported(true);
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicReference<AsyncContext> contextReference = new AtomicReference<>();

            filter.doFilter(request, response, (servletRequest, servletResponse) ->
                    contextReference.set(servletRequest.startAsync()));

            assertEquals(1.0d, reading(registry.snapshot(), "http.server.active.requests").value());
            contextReference.get().complete();

            assertEquals(1L, reading(registry.snapshot(), "http.server.requests").count());
            assertEquals(0.0d, reading(registry.snapshot(), "http.server.active.requests").value());
        }
    }

    private static MetricReading reading(MetricSnapshot snapshot, String name) {
        return snapshot.readings().stream()
                .filter(reading -> name.equals(reading.binding().getSpec().getName().getValue()))
                .findFirst()
                .orElseThrow();
    }

    private static final class CompletingAsyncRequest extends MockHttpServletRequest {
        private AsyncContext asyncContext;

        private CompletingAsyncRequest(String method, String requestUri) {
            super(method, requestUri);
        }

        @Override
        public AsyncContext startAsync() {
            return startAsync(this, null);
        }

        @Override
        public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
            asyncContext = new CompletingAsyncContext(request, response);
            return asyncContext;
        }

        @Override
        public boolean isAsyncStarted() {
            return asyncContext != null;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return asyncContext;
        }
    }

    private static final class CompletingAsyncContext implements AsyncContext {
        private final ServletRequest request;
        private final ServletResponse response;
        private final List<AsyncListener> listeners = new ArrayList<>();

        private CompletingAsyncContext(ServletRequest request, ServletResponse response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public ServletRequest getRequest() {
            return request;
        }

        @Override
        public ServletResponse getResponse() {
            return response;
        }

        @Override
        public boolean hasOriginalRequestAndResponse() {
            return true;
        }

        @Override
        public void dispatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispatch(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispatch(ServletContext context, String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void complete() {
            AsyncEvent event = new AsyncEvent(this);
            for (AsyncListener listener : List.copyOf(listeners)) {
                try {
                    listener.onComplete(event);
                } catch (IOException failure) {
                    throw new IllegalStateException(failure);
                }
            }
        }

        @Override
        public void start(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void addListener(AsyncListener listener) {
            listeners.add(listener);
        }

        @Override
        public void addListener(
                AsyncListener listener,
                ServletRequest request,
                ServletResponse response
        ) {
            listeners.add(listener);
        }

        @Override
        public <T extends AsyncListener> T createListener(Class<T> listenerClass)
                throws ServletException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTimeout(long timeout) {
        }

        @Override
        public long getTimeout() {
            return 0L;
        }
    }
}
