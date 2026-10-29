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

import com.lamprism.luxspec.network.ip.IpAddress;
import com.lamprism.luxspec.security.authentication.Subject;
import org.jspecify.annotations.Nullable;

/**
 * Exposes optional request facts consumed by common firewalls.
 *
 * <p>The boundary adapter is responsible for supplying the operation method, canonical resource
 * path, and normalized client address used by both routing and firewall policy. HTTP adapters map
 * these facts from an HTTP request, while another transport may map equivalent operation facts
 * without exposing its provider request object.</p>
 *
 * @author RollW
 */
public interface FirewallRequest {
    /**
     * Returns the request operation method.
     *
     * @return the operation method
     */
    default @Nullable String getMethod() {
        return null;
    }

    /**
     * Returns the canonical resource path used by routing and firewall policy.
     *
     * @return the canonical resource path
     */
    default @Nullable String getPath() {
        return null;
    }

    /**
     * Returns the normalized client address supplied by the trusted boundary adapter.
     *
     * @return the normalized client address
     */
    default @Nullable IpAddress getClientAddress() {
        return null;
    }

    /**
     * Returns the authenticated subject when this request has already crossed authentication.
     *
     * @return the subject, or {@code null} before authentication
     */
    default @Nullable Subject getSubject() {
        return null;
    }
}
