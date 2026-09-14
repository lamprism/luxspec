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

package com.lamprism.luxspec.web.spring;

import com.lamprism.luxspec.context.CorrelationId;
import com.lamprism.luxspec.context.CorrelationIdGenerator;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.UuidCorrelationIdGenerator;
import com.lamprism.luxspec.context.slf4j.Slf4jMdcScope;
import com.lamprism.luxspec.web.WebRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Creates immutable Web request context facts and returns one correlation identifier around one
 * servlet request.
 *
 * <p>Context lookup is optional. When an {@link ExecutionContextStorage} is supplied, the filter
 * opens the derived request context around the downstream chain. The filter does not select a
 * storage implementation itself.</p>
 *
 * @author RollW
 */
public class LuxspecRequestContextFilter extends OncePerRequestFilter implements Ordered {
    private static final String DEFAULT_CORRELATION_ID_HEADER = "X-Request-ID";

    private final String correlationIdHeader;
    private final CorrelationIdGenerator correlationIdGenerator;
    private final @Nullable ExecutionContextStorage storage;

    /**
     * Creates a filter with the default correlation ID header and UUID generator.
     */
    public LuxspecRequestContextFilter() {
        this(DEFAULT_CORRELATION_ID_HEADER, new UuidCorrelationIdGenerator(), null);
    }

    /**
     * Creates a filter with explicit correlation ID dependencies.
     *
     * @param correlationIdHeader    the request and response header name
     * @param correlationIdGenerator the generator used when the request header is absent or invalid
     */
    public LuxspecRequestContextFilter(
            String correlationIdHeader,
            CorrelationIdGenerator correlationIdGenerator
    ) {
        this(correlationIdHeader, correlationIdGenerator, null);
    }

    /**
     * Creates a filter with explicit correlation and optional context-storage dependencies.
     *
     * @param correlationIdHeader    the request and response header name
     * @param correlationIdGenerator the generator used when the request header is absent or invalid
     * @param storage                the optional context storage used around the downstream chain
     */
    public LuxspecRequestContextFilter(
            String correlationIdHeader,
            CorrelationIdGenerator correlationIdGenerator,
            @Nullable ExecutionContextStorage storage
    ) {
        this.correlationIdHeader = requireHeaderName(correlationIdHeader);
        this.correlationIdGenerator = Objects.requireNonNull(
                correlationIdGenerator,
                "correlationIdGenerator"
        );
        this.storage = storage;
    }

    /**
     * Runs before the Security filter chain so the response correlation header and request context
     * are available early.
     *
     * @return the highest servlet filter precedence
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CorrelationId correlationId = resolveCorrelationId(request.getHeader(correlationIdHeader));
        response.setHeader(correlationIdHeader, correlationId.value());
        ExecutionContext context = ExecutionContext.empty()
                .with(WebRequestContext.KEY, new WebRequestContext(
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getLocale()
                ))
                .with(ExecutionContext.CORRELATION_ID, correlationId);
        if (storage == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try (ExecutionContextStorage.Scope ignored = storage.open(context);
             Slf4jMdcScope mdcScope = Slf4jMdcScope.open(context)) {
            filterChain.doFilter(request, response);
        }
    }

    private CorrelationId resolveCorrelationId(@Nullable String headerValue) {
        if (headerValue == null) {
            return generatedCorrelationId();
        }
        try {
            return CorrelationId.of(headerValue);
        } catch (IllegalArgumentException exception) {
            return generatedCorrelationId();
        }
    }

    private CorrelationId generatedCorrelationId() {
        return Objects.requireNonNull(
                correlationIdGenerator.generate(),
                "generated correlationId"
        );
    }

    private static String requireHeaderName(String value) {
        String nonNullValue = Objects.requireNonNull(value, "correlationIdHeader").trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException("correlationIdHeader must not be blank");
        }
        return nonNullValue;
    }
}
