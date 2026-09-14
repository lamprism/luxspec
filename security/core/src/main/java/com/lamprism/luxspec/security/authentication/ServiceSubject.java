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

package com.lamprism.luxspec.security.authentication;

import java.util.Objects;

/**
 * Identifies one machine or API-client actor.
 *
 * @author RollW
 */
public final class ServiceSubject implements Subject {
    static final String TYPE = "service";

    private final String serviceId;

    /**
     * Creates a service subject with a stable non-blank identifier.
     *
     * @param serviceId the machine or API-client identifier
     */
    public ServiceSubject(String serviceId) {
        this.serviceId = requireIdentifier(serviceId);
    }

    /**
     * Returns the stable service identifier.
     *
     * @return the service identifier
     */
    public String serviceId() {
        return serviceId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getId() {
        return serviceId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ServiceSubject subject)) {
            return false;
        }
        return serviceId.equals(subject.serviceId);
    }

    @Override
    public int hashCode() {
        return serviceId.hashCode();
    }

    private static String requireIdentifier(String value) {
        String nonNullValue = Objects.requireNonNull(value, "serviceId");
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
        for (int index = 0; index < nonNullValue.length(); index++) {
            if (Character.isWhitespace(nonNullValue.charAt(index))) {
                throw new IllegalArgumentException("serviceId must not contain whitespace");
            }
        }
        return nonNullValue;
    }
}
