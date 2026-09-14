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

package com.lamprism.luxspec.security.spring.firewall;

import com.lamprism.luxspec.security.firewall.FirewallChain;
import com.lamprism.luxspec.security.firewall.FirewallDecision;
import com.lamprism.luxspec.security.firewall.IngressRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuxspecIngressFirewallFilterTest {
    @Test
    void passesTheConfiguredClientAddressIntoTheIngressFirewall() throws Exception {
        AtomicReference<IngressRequest> observedRequest = new AtomicReference<>();
        FirewallChain<IngressRequest> firewallChain = new FirewallChain<>(
                List.of(request -> {
                    observedRequest.set(request);
                    return FirewallDecision.pass();
                })
        );
        LuxspecIngressFirewallFilter filter = new LuxspecIngressFirewallFilter(
                firewallChain,
                request -> "198.51.100.42",
                (ignoredRequest, ignoredResponse, decision) -> {
                    throw new AssertionError("Passing firewall decision must not be handled");
                }
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> chainReached.set(true));

        IngressRequest ingressRequest = observedRequest.get();
        assertNotNull(ingressRequest);
        assertEquals("POST", ingressRequest.getMethod());
        assertEquals("/orders", ingressRequest.getPath());
        assertEquals("198.51.100.42", ingressRequest.getClientAddress());
        assertTrue(chainReached.get());
    }
}
