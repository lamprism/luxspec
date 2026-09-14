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

import com.lamprism.luxspec.validation.Validator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable default observation link for applications that do not need a provider-specific type.
 *
 * @author RollW
 */
public class SimpleObservationLink implements ObservationLink {
    private static final int MAXIMUM_IDENTIFIER_LENGTH = 256;
    private static final int MAXIMUM_ATTRIBUTE_LENGTH = 1_024;
    private final String traceId;
    private final String spanId;
    private final Map<String, String> attributes;

    /**
     * Creates a link without attributes.
     *
     * @param traceId the non-blank trace identity
     * @param spanId  the non-blank span identity
     */
    public SimpleObservationLink(String traceId, String spanId) {
        this(traceId, spanId, Map.of());
    }

    /**
     * Creates a link with validated immutable attributes.
     *
     * @param traceId    the non-blank trace identity
     * @param spanId     the non-blank span identity
     * @param attributes the link attributes
     */
    public SimpleObservationLink(
            String traceId,
            String spanId,
            Map<String, String> attributes
    ) {
        this.traceId = requireIdentifier(traceId, "traceId");
        this.spanId = requireIdentifier(spanId, "spanId");
        this.attributes = copyAttributes(attributes);
    }

    @Override
    public String traceId() {
        return traceId;
    }

    /**
     * Returns the trace identity.
     *
     * @return the trace identity
     */
    public String getTraceId() {
        return traceId;
    }

    @Override
    public String spanId() {
        return spanId;
    }

    /**
     * Returns the span identity.
     *
     * @return the span identity
     */
    public String getSpanId() {
        return spanId;
    }

    @Override
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * Returns the immutable link attributes.
     *
     * @return the link attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ObservationLink link)) {
            return false;
        }
        return traceId.equals(link.traceId())
                && spanId.equals(link.spanId())
                && attributes.equals(link.attributes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId, attributes);
    }

    @Override
    public String toString() {
        return "ObservationLink[traceId=" + traceId + ", spanId=" + spanId + "]";
    }

    private static String requireIdentifier(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (nonNullValue.length() > MAXIMUM_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException(name + " exceeds the maximum length");
        }
        Validator.noControlCharacters(name).validate(nonNullValue);
        return nonNullValue;
    }

    private static Map<String, String> copyAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes");
        if (attributes.size() > 32) {
            throw new IllegalArgumentException("Observation link attributes exceed the maximum count");
        }
        Map<String, String> copied = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String name = requireAttributeName(entry.getKey());
            String value = requireAttributeValue(entry.getValue());
            copied.put(name, value);
        }
        return Map.copyOf(copied);
    }

    private static String requireAttributeName(String value) {
        String nonNullValue = Objects.requireNonNull(value, "attribute name");
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException("attribute name must not be empty");
        }
        if (nonNullValue.length() > MAXIMUM_ATTRIBUTE_LENGTH) {
            throw new IllegalArgumentException("attribute name exceeds the maximum length");
        }
        Validator.noWhitespace("attribute name")
                .and(Validator.noControlCharacters("attribute name"))
                .validate(nonNullValue);
        return nonNullValue;
    }

    private static String requireAttributeValue(String value) {
        String nonNullValue = Objects.requireNonNull(value, "attribute value");
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException("attribute value must not be empty");
        }
        if (nonNullValue.length() > MAXIMUM_ATTRIBUTE_LENGTH) {
            throw new IllegalArgumentException("attribute value exceeds the maximum length");
        }
        Validator.noControlCharacters("attribute value").validate(nonNullValue);
        return nonNullValue;
    }
}
