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

import com.lamprism.luxspec.context.ContextKey;
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
public final class LuxspecRequestContextFilter extends OncePerRequestFilter {
    private static final String TRACE_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CorrelationId correlationId = resolveCorrelationId(request.getHeader(TRACE_ID_HEADER));
        response.setHeader(TRACE_ID_HEADER, correlationId.value());
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
        ExecutionContext currentContext = ExecutionContexts.current().orElse(ExecutionContext.empty());
        ExecutionContext withRequest = addOrReplace(
                currentContext,
                WebContextKeys.REQUEST,
                requestContext
        );
        return addOrReplace(withRequest, WebContextKeys.CORRELATION_ID, correlationId);
    }

    private static CorrelationId resolveCorrelationId(String headerValue) {
        if (headerValue == null) {
            return CorrelationId.generated();
        }
        try {
            return CorrelationId.of(headerValue);
        } catch (IllegalArgumentException exception) {
            return CorrelationId.generated();
        }
    }

    private static <T> ExecutionContext addOrReplace(
            ExecutionContext context,
            ContextKey<T> key,
            T value
    ) {
        if (context.get(key).isPresent()) {
            return context.replace(key, value);
        }
        return context.with(key, value);
    }
}
