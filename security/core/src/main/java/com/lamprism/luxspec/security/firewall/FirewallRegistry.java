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

import java.util.List;

/**
 * Dynamically editable, priority-ordered firewall registry.
 *
 * @param <R> the request type filtered by the registry
 *
 * @author RollW
 */
public interface FirewallRegistry<R extends FirewallRequest> {
    /**
     * Returns an immutable snapshot of registered firewalls in execution order.
     *
     * @return the registered firewalls
     */
    List<Firewall<? super R>> getFirewalls();

    /**
     * Adds one firewall to the registry.
     *
     * @param firewall the firewall to add
     */
    void addFirewall(Firewall<? super R> firewall);

    /**
     * Removes one firewall from the registry.
     *
     * @param firewall the firewall to remove
     * @return {@code true} when the firewall was registered
     */
    boolean removeFirewall(Firewall<? super R> firewall);

    /**
     * Filters a request through the current registry snapshot.
     *
     * @param request the request facts
     * @return the first allow or deny decision, or neutral when every firewall is neutral
     */
    FirewallDecision filter(R request);

    /**
     * Clears request-specific state in every registered firewall.
     *
     * @param request request facts identifying the state
     */
    void clearContext(R request);

    /**
     * Clears all state in every registered firewall.
     */
    void clearAllContext();
}
