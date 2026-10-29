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
import com.lamprism.luxspec.web.HttpResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Fills an absent response-envelope correlation ID from the current Servlet request.
 *
 * <p>The request attribute also remains available across Servlet async dispatch. Non-envelope
 * bodies and explicitly supplied correlation IDs are left unchanged.</p>
 *
 * @author RollW
 */
@ControllerAdvice
public final class LuxspecCorrelationIdResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(
            @Nullable Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!(body instanceof HttpResponse<?> envelope) || envelope.correlationId() != null
                || !(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        Object value = servletRequest.getServletRequest().getAttribute(LuxspecRequestContextFilter.CORRELATION_ID_ATTRIBUTE);
        if (!(value instanceof CorrelationId correlationId)) {
            return body;
        }
        return new HttpResponse<>(envelope.status(), envelope.data(), correlationId.value());
    }
}
