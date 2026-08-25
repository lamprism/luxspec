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
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.web.WebContextKeys;
import com.lamprism.luxspec.web.WebRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuxspecRequestContextFilterTest {
    @Test
    void runsBeforeSecurityFilters() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE, new LuxspecRequestContextFilter().getOrder());
    }

    @Test
    void returnsTheCorrelationHeaderWithoutSelectingStorage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/accounts");
        request.addHeader("X-Request-ID", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertEquals(CorrelationId.of("trace-123"), CorrelationId.of(response.getHeader("X-Request-ID")));
        assertEquals("trace-123", response.getHeader("X-Request-ID"));
    }

    @Test
    void opensTheExplicitRequestContextWhenConfigured() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/accounts");
        request.addHeader("X-Request-ID", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter(
                "X-Request-ID",
                () -> CorrelationId.of("generated-id"),
                storage
        );

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ExecutionContext context = storage.requireCurrent();
            assertEquals(
                    new WebRequestContext("POST", "/accounts", request.getLocale()),
                    context.get(WebContextKeys.REQUEST).orElseThrow()
            );
            assertEquals(
                    CorrelationId.of("trace-123"),
                    context.get(WebContextKeys.CORRELATION_ID).orElseThrow()
            );
        });

        assertTrue(storage.current().isEmpty());
    }

    @Test
    void propagatesTheChainFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter();
        RuntimeException failure = new RuntimeException("expected");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                    throw failure;
                })
        );

        assertSame(failure, thrown);
    }

    @Test
    void replacesAnInvalidIncomingCorrelationIdWithASafeGeneratedValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts");
        request.addHeader("X-Request-ID", "invalid trace id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        String responseCorrelationId = response.getHeader("X-Request-ID");
        assertNotEquals("invalid trace id", responseCorrelationId);
        CorrelationId.of(responseCorrelationId);
    }

    @Test
    void usesTheConfiguredHeaderAndCorrelationIdGenerator() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter(
                "X-Correlation-ID",
                () -> CorrelationId.of("generated-id")
        );

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertEquals("generated-id", response.getHeader("X-Correlation-ID"));
    }
}
