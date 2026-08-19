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

package com.lamprism.luxspec.observability.observation;

import java.util.Map;

/**
 * Provider-neutral link context supplied when an observation starts.
 *
 * <p>The identifiers are deliberately not interpreted as a particular tracing vendor's format.
 * An adapter may translate them to its native link type, or preserve the complete value in its
 * provider context for a tracing handler.</p>
 *
 * @author RollW
 */
public interface ObservationLink {
    /**
     * Creates a link without additional attributes.
     *
     * @param traceId the non-blank trace identity
     * @param spanId  the non-blank span identity
     * @return the immutable link
     */
    static ObservationLink of(String traceId, String spanId) {
        return new SimpleObservationLink(traceId, spanId, Map.of());
    }

    /**
     * Creates a link with provider-neutral attributes.
     *
     * @param traceId    the non-blank trace identity
     * @param spanId     the non-blank span identity
     * @param attributes the optional link attributes
     * @return the immutable link
     */
    static ObservationLink of(String traceId, String spanId, Map<String, String> attributes) {
        return new SimpleObservationLink(traceId, spanId, attributes);
    }

    /**
     * Returns the opaque trace identity.
     *
     * @return the trace identity
     */
    String traceId();

    /**
     * Returns the opaque span identity.
     *
     * @return the span identity
     */
    String spanId();

    /**
     * Returns immutable link attributes.
     *
     * @return the link attributes
     */
    Map<String, String> attributes();
}
