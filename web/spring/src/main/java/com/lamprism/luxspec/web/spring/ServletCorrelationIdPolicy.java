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
import jakarta.servlet.http.HttpServletRequest;

/**
 * Selects a correlation identifier for one Servlet request.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ServletCorrelationIdPolicy {
    /**
     * Accepts a valid incoming header or generates an ID when it is absent or invalid.
     *
     * @return the incoming-or-generated policy
     */
    static ServletCorrelationIdPolicy incomingOrGenerated() {
        return (request, header, generator) -> {
            String incoming = request.getHeader(header);
            if (incoming != null) {
                try {
                    return CorrelationId.of(incoming);
                } catch (IllegalArgumentException ignored) {
                    return generator.generate();
                }
            }
            return generator.generate();
        };
    }

    /**
     * Generates a new ID without using the incoming header.
     *
     * @return the server-generated policy
     */
    static ServletCorrelationIdPolicy generated() {
        return (request, header, generator) -> generator.generate();
    }

    /**
     * Selects the request ID.
     *
     * @param request   the current request
     * @param header    the configured incoming header name
     * @param generator the configured ID generator
     * @return the selected validated correlation ID
     */
    CorrelationId resolve(HttpServletRequest request, String header, CorrelationIdGenerator generator);
}
