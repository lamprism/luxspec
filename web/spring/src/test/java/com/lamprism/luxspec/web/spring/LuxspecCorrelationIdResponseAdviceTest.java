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
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.web.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class LuxspecCorrelationIdResponseAdviceTest {
    @Test
    void fillsAnEnvelopeAfterTheRequestContextHasClosed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ThreadLocalExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        LuxspecRequestContextFilter filter = new LuxspecRequestContextFilter(
                "X-Request-ID", () -> CorrelationId.of("server-id"), storage, ServletCorrelationIdPolicy.generated()
        );
        request.setAsyncSupported(true);
        filter.doFilter(request, response, (servletRequest, servletResponse) -> request.startAsync());
        assertTrue(storage.current().isEmpty());

        HttpResponse<?> result = (HttpResponse<?>) write(HttpResponse.success("ok", null), request, response);
        assertEquals("server-id", result.correlationId());
    }

    @Test
    void preservesExplicitIdsAndOtherResponseBodies() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new LuxspecRequestContextFilter().doFilter(request, response, (servletRequest, servletResponse) -> {
        });
        HttpResponse<String> explicit = HttpResponse.success("ok", "application-id");
        Object body = new Object();

        assertSame(explicit, write(explicit, request, response));
        assertSame(body, write(body, request, response));
    }

    private static Object write(Object body, MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        MethodParameter returnType = new MethodParameter(
                LuxspecCorrelationIdResponseAdviceTest.class.getDeclaredMethod("exampleResponse"), -1
        );
        return new LuxspecCorrelationIdResponseAdvice().beforeBodyWrite(
                body, returnType, MediaType.APPLICATION_JSON, StringHttpMessageConverter.class,
                new ServletServerHttpRequest(request), new ServletServerHttpResponse(response)
        );
    }

    private static HttpResponse<String> exampleResponse() {
        return HttpResponse.success("ok", null);
    }
}
