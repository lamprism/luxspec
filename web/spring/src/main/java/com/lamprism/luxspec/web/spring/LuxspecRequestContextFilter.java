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
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.context.Slf4jMdcScope;
import com.lamprism.luxspec.web.WebContextKeys;
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
 * Creates and cleans the immutable Web request scope around one servlet request.
 *
 * <p>The filter retains request facts and a safe correlation identifier, never the mutable servlet
 * request or response objects.</p>
 *
 * @author RollW
 */
public class LuxspecRequestContextFilter extends OncePerRequestFilter implements Ordered {
    private static final String DEFAULT_CORRELATION_ID_HEADER = "X-Request-ID";

    private final String correlationIdHeader;
    private final CorrelationIdGenerator correlationIdGenerator;

    /**
     * Creates a filter with the default correlation ID header and UUID generator.
     */
    public LuxspecRequestContextFilter() {
        this(DEFAULT_CORRELATION_ID_HEADER, new UuidCorrelationIdGenerator());
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
        this.correlationIdHeader = requireHeaderName(correlationIdHeader);
        this.correlationIdGenerator = Objects.requireNonNull(
                correlationIdGenerator,
                "correlationIdGenerator"
        );
    }

    /**
     * Runs before the Security filter chain so downstream authentication augments the request root.
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
        ExecutionContext context = createContext(request, correlationId);
        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(context);
             Slf4jMdcScope mdcScope = Slf4jMdcScope.open(context)) {
            filterChain.doFilter(request, response);
        }
    }

    private static ExecutionContext createContext(
            HttpServletRequest request,
            CorrelationId correlationId
    ) {
        HttpServletRequest nonNullRequest = Objects.requireNonNull(request, "request");
        WebRequestContext requestContext = new WebRequestContext(
                nonNullRequest.getMethod(),
                nonNullRequest.getRequestURI(),
                nonNullRequest.getLocale()
        );
        return ExecutionContext.empty()
                .with(WebContextKeys.REQUEST, requestContext)
                .with(WebContextKeys.CORRELATION_ID, correlationId);
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
