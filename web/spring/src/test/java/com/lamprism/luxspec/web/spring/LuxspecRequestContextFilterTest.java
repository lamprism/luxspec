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
import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.web.WebContextKeys;
import com.lamprism.luxspec.web.WebRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuxspecRequestContextFilterTest {
    @Test
    void opensRequestAndCorrelationValuesAndReturnsTheCorrelationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/accounts");
        request.addHeader("X-Request-ID", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter();
        AtomicReference<WebRequestContext> requestContext = new AtomicReference<>();
        AtomicReference<CorrelationId> correlationId = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            requestContext.set(ExecutionContexts.requireCurrent()
                    .get(WebContextKeys.REQUEST)
                    .orElseThrow());
            correlationId.set(ExecutionContexts.requireCurrent()
                    .get(WebContextKeys.CORRELATION_ID)
                    .orElseThrow());
        });

        assertEquals(new WebRequestContext("POST", "/accounts", request.getLocale()), requestContext.get());
        assertEquals(CorrelationId.of("trace-123"), correlationId.get());
        assertEquals("trace-123", response.getHeader("X-Request-ID"));
        assertTrue(ExecutionContexts.current().isEmpty());
    }

    @Test
    void preservesAnOuterContextAndCleansUpWhenTheChainFails() throws Exception {
        ContextKey<String> outerKey = ContextKey.of("outer", String.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter();
        RuntimeException failure = new RuntimeException("expected");

        try (ExecutionContexts.Scope outerScope = ExecutionContexts.open(
                com.lamprism.luxspec.context.ExecutionContext.empty().with(outerKey, "outer-value")
        )) {
            RuntimeException thrown = assertThrows(
                    RuntimeException.class,
                    () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                        assertEquals("outer-value", ExecutionContexts.requireCurrent()
                                .get(outerKey)
                                .orElseThrow());
                        throw failure;
                    })
            );

            assertSame(failure, thrown);
            assertFalse(ExecutionContexts.requireCurrent().get(WebContextKeys.REQUEST).isPresent());
            assertEquals("outer-value", ExecutionContexts.requireCurrent().get(outerKey).orElseThrow());
        }
        assertTrue(ExecutionContexts.current().isEmpty());
    }

    @Test
    void replacesAnInvalidIncomingCorrelationIdWithASafeGeneratedValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts");
        request.addHeader("X-Request-ID", "invalid trace id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertTrue(ExecutionContexts.requireCurrent().get(WebContextKeys.CORRELATION_ID).isPresent());
        });

        String responseCorrelationId = response.getHeader("X-Request-ID");
        assertNotEquals("invalid trace id", responseCorrelationId);
        CorrelationId.of(responseCorrelationId);
        assertTrue(ExecutionContexts.current().isEmpty());
    }
}
