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

import com.lamprism.luxspec.observability.observation.Observation;
import com.lamprism.luxspec.observability.observation.ObservationAttributeClassification;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSpec;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationScope;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationCatalog;
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
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captures one provider-independent observation for each Servlet request.
 *
 * <p>The filter is opt-in and shares the standard {@code web.request} declaration with OTLP and
 * Micrometer adapters. Async requests remain active until the Servlet async context completes.</p>
 *
 * @author RollW
 */
public final class LuxspecHttpObservationFilter extends OncePerRequestFilter implements Ordered {
    static final ObservationAttributeSpec<String> METHOD = ObservationAttributeSpec.string(
            "http.request.method",
            ObservationAttributeClassification.LOW
    );
    static final ObservationAttributeSpec<String> ROUTE = ObservationAttributeSpec.string(
            "http.route",
            ObservationAttributeClassification.LOW
    );
    static final ObservationAttributeSpec<String> STATUS = ObservationAttributeSpec.string(
            "http.response.status_code",
            ObservationAttributeClassification.LOW
    );

    private final ObservationRegistry registry;
    private final HttpObservationOutcomePolicy outcomePolicy;

    /**
     * Creates an HTTP observation filter.
     *
     * @param registry the registry containing the standard web request specification
     */
    public LuxspecHttpObservationFilter(ObservationRegistry registry) {
        this(registry, HttpObservationOutcomePolicy.exceptionsOnly());
    }

    /**
     * Creates an HTTP observation filter with an explicit outcome policy.
     *
     * @param registry      the observation registry
     * @param outcomePolicy the policy for completed HTTP requests
     */
    public LuxspecHttpObservationFilter(ObservationRegistry registry, HttpObservationOutcomePolicy outcomePolicy) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.outcomePolicy = Objects.requireNonNull(outcomePolicy, "outcomePolicy");
    }

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
        Observation observation = registry.start(StandardObservationCatalog.WEB_REQUEST);
        observation.put(METHOD, method(request.getMethod()));
        RequestCompletion completion = new RequestCompletion(observation, request, response);
        try (ObservationScope ignored = observation.openScope()) {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error failure) {
            completion.complete(failure, true);
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
        if (value == null || value.isEmpty() || value.length() > 32) {
            return "UNKNOWN";
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!(character >= 'A' && character <= 'Z')
                    && !(character >= 'a' && character <= 'z')
                    && !(character >= '0' && character <= '9')
                    && character != '-') {
                return "UNKNOWN";
            }
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static String route(HttpServletRequest request) {
        Object value = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (!(value instanceof String route) || route.isBlank() || route.length() > 256) {
            return "UNKNOWN";
        }
        for (int index = 0; index < route.length(); index++) {
            if (Character.isISOControl(route.charAt(index))) {
                return "UNKNOWN";
            }
        }
        return route;
    }

    private static String status(int value) {
        if (value < 100 || value > 999) {
            return "UNKNOWN";
        }
        return Integer.toString(value);
    }

    private final class RequestCompletion implements AsyncListener {
        private final Observation observation;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final AtomicBoolean completed = new AtomicBoolean();

        private RequestCompletion(
                Observation observation,
                HttpServletRequest request,
                HttpServletResponse response
        ) {
            this.observation = observation;
            this.request = request;
            this.response = response;
        }

        private boolean isComplete() {
            return completed.get();
        }

        private void register() {
            try {
                request.getAsyncContext().addListener(this);
            } catch (IllegalStateException ignored) {
                complete(null, true);
            }
        }

        private void complete(@Nullable Throwable failure, boolean forcedFailure) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            if (failure != null) {
                observation.error(failure);
            }
            boolean failed = forcedFailure || failure != null;
            int responseStatus = response.getStatus();
            int effectiveStatus = failed && (responseStatus < 500 || responseStatus > 599) ? 500 : responseStatus;
            observation.put(ROUTE, route(request));
            observation.put(STATUS, status(effectiveStatus));
            observation.setOutcome(Objects.requireNonNull(outcomePolicy.resolve(effectiveStatus, failed), "outcome"));
            observation.stop();
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
            register();
        }
    }
}
