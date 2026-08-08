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

package com.lamprism.luxspec.security.firewall;

import java.util.Objects;

/**
 * Represents framework-independent facts available before authentication.
 *
 * @author RollW
 */
public final class IngressRequest {
    private final String method;
    private final String path;
    private final String clientAddress;

    /**
     * Creates immutable ingress facts supplied by a trusted boundary adapter.
     *
     * @param method        the request method
     * @param path          the request path
     * @param clientAddress the normalized client address
     */
    public IngressRequest(String method, String path, String clientAddress) {
        this.method = requireText(method, "method");
        this.path = requireText(path, "path");
        this.clientAddress = requireText(clientAddress, "clientAddress");
    }

    /**
     * Returns the request method.
     *
     * @return the request method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the request path.
     *
     * @return the request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the normalized client address.
     *
     * @return the client address
     */
    public String getClientAddress() {
        return clientAddress;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
