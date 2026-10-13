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

/**
 * Evaluates one independent firewall against a request.
 *
 * <p>A firewall should be thread-safe. A neutral result leaves the request available to lower
 * priority firewalls, while an allow or deny result terminates registry evaluation.</p>
 *
 * @param <R> the request type accepted by the firewall
 * @author RollW
 */
public interface Firewall<R extends FirewallRequest> {
    /**
     * Verifies one request.
     *
     * @param request immutable request facts
     * @return the three-state firewall decision
     */
    FirewallDecision verifyRequest(R request);

    /**
     * Clears state associated with one request identity.
     *
     * @param request request facts identifying state to clear
     */
    default void clearContext(R request) {
    }

    /**
     * Clears all state owned by this firewall.
     */
    default void clearAllContext() {
    }

    /**
     * Returns the registry ordering priority. Higher values run first.
     *
     * @return the firewall priority
     */
    default int getPriority() {
        return 0;
    }
}
