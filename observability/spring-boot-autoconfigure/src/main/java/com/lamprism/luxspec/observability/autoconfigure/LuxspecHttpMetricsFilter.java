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

import com.lamprism.luxspec.observability.metric.MetricDimensionSet;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.Timer;
import com.lamprism.luxspec.observability.runtime.metric.HttpServerMetricSet;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Collects inbound Servlet request metrics for registered HTTP metric definitions.
 *
 * <p>The filter records one request after the response completes. It uses the Spring MVC best
 * matching route template instead of the raw request URI. A missing route template is represented
 * by {@link HttpServerMetricSet#UNKNOWN}. Metric recording is best effort and never replaces a
 * business response or exception.</p>
 *
 * @author RollW
 */
public class LuxspecHttpMetricsFilter extends OncePerRequestFilter implements Ordered {
    private final MetricRegistry registry;
    private final AtomicLong activeRequests = new AtomicLong();
    private final LongSupplier activeRequestReader = activeRequests::get;

    /**
     * Creates a request metrics filter.
     *
     * @param registry the metric registry receiving request readings and containing the registered
     *                 HTTP metric definitions
     */
    public LuxspecHttpMetricsFilter(MetricRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        registry.obtain(HttpServerMetricSet.ACTIVE_REQUESTS.bind(
                MetricDimensionSet.empty(),
                activeRequestReader
        ));
    }

    /**
     * Runs after the request-context filter and before ordinary application filters.
     *
     * @return the filter order
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RequestCompletion completion = new RequestCompletion(request, response, System.nanoTime());
        activeRequests.incrementAndGet();
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error failure) {
            completion.complete(failure, false);
            throw failure;
        } finally {
            if (!request.isAsyncStarted()) {
                completion.complete(null, false);
            } else if (!completion.isComplete()) {
                completion.register();
            }
        }
    }

    private static String method(@Nullable String value) {
        if (value == null || value.isEmpty()
                || value.length() > HttpServerMetricSet.MAXIMUM_METHOD_LENGTH) {
            return HttpServerMetricSet.UNKNOWN;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!isMethodCharacter(character)) {
                return HttpServerMetricSet.UNKNOWN;
            }
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static String route(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (!(attribute instanceof String value)
                || value.isBlank()
                || value.length() > HttpServerMetricSet.MAXIMUM_ROUTE_LENGTH) {
            return HttpServerMetricSet.UNKNOWN;
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return HttpServerMetricSet.UNKNOWN;
            }
        }
        return value;
    }

    private static String status(int value) {
        if (value < 100 || value > 999) {
            return HttpServerMetricSet.UNKNOWN;
        }
        return Integer.toString(value);
    }

    private static String outcome(int status) {
        if (status < 100 || status > 999) {
            return HttpServerMetricSet.UNKNOWN;
        }
        if (status >= 100 && status < 200) {
            return "INFORMATIONAL";
        }
        if (status < 300) {
            return "SUCCESS";
        }
        if (status < 400) {
            return "REDIRECTION";
        }
        if (status < 500) {
            return "CLIENT_ERROR";
        }
        if (status < 600) {
            return "SERVER_ERROR";
        }
        return HttpServerMetricSet.UNKNOWN;
    }

    private static int effectiveStatus(int responseStatus, boolean failed) {
        if (failed && (responseStatus < 500 || responseStatus > 599)) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        return responseStatus;
    }

    private static boolean isMethodCharacter(char character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == '-';
    }

    private final class RequestCompletion implements AsyncListener {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final long startedAtNanos;
        private final AtomicBoolean completed = new AtomicBoolean();

        private RequestCompletion(
                HttpServletRequest request,
                HttpServletResponse response,
                long startedAtNanos
        ) {
            this.request = request;
            this.response = response;
            this.startedAtNanos = startedAtNanos;
        }

        private boolean isComplete() {
            return completed.get();
        }

        private void register() {
            try {
                register(request.getAsyncContext());
            } catch (IllegalStateException ignored) {
                complete(null, false);
            }
        }

        private void register(AsyncContext context) {
            try {
                context.addListener(this);
            } catch (IllegalStateException ignored) {
                complete(null, false);
            }
        }

        private void complete(@Nullable Throwable failure, boolean forcedFailure) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            activeRequests.decrementAndGet();
            int responseStatus = effectiveStatus(response.getStatus(), failure != null || forcedFailure);
            try {
                recordRequest(request, responseStatus, elapsed());
            } catch (RuntimeException ignored) {
                // Metrics must not change the outcome of the application request.
            }
        }

        private void recordRequest(
                HttpServletRequest request,
                int responseStatus,
                Duration duration
        ) {
            Timer timer = registry.obtain(HttpServerMetricSet.REQUESTS.bind(
                    MetricDimensionSet.builder()
                            .put(HttpServerMetricSet.METHOD, method(request.getMethod()))
                            .put(HttpServerMetricSet.ROUTE, route(request))
                            .put(HttpServerMetricSet.STATUS, status(responseStatus))
                            .put(HttpServerMetricSet.OUTCOME, outcome(responseStatus))
                            .build()
            ));
            timer.record(duration);
        }

        private Duration elapsed() {
            long elapsedNanos = System.nanoTime() - startedAtNanos;
            return Duration.ofNanos(Math.max(0L, elapsedNanos));
        }

        @Override
        public void onComplete(AsyncEvent event) {
            complete(null, false);
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            complete(null, true);
        }

        @Override
        public void onError(AsyncEvent event) {
            complete(event.getThrowable(), true);
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            if (!isComplete()) {
                register(event.getAsyncContext());
            }
        }
    }
}
