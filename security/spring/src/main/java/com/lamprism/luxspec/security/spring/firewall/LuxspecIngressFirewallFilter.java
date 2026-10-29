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

import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.firewall.FirewallDecision;
import com.lamprism.luxspec.security.firewall.FirewallRegistry;
import com.lamprism.luxspec.security.firewall.IngressRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Applies an ingress Firewall registry before authentication and downstream processing.
 *
 * <p>The filter is explicit and is not installed by security auto-configuration. Applications
 * choose its position relative to authentication filters.</p>
 *
 * @author RollW
 */
public class LuxspecIngressFirewallFilter extends OncePerRequestFilter {
    private final FirewallRegistry<IngressRequest> firewallRegistry;
    private final ServletClientAddressResolver clientAddressResolver;
    private final FirewallDecisionHandler decisionHandler;

    /**
     * Creates an ingress filter with Servlet remote-address resolution and HTTP 403 denials.
     *
     * @param firewallRegistry the ingress Firewall registry
     */
    public LuxspecIngressFirewallFilter(FirewallRegistry<IngressRequest> firewallRegistry) {
        this(
                firewallRegistry,
                ServletClientAddressResolver.remoteAddress(),
                new HttpStatusFirewallDecisionHandler()
        );
    }

    /**
     * Creates an ingress filter with explicit client-address resolution and denial handling.
     *
     * @param firewallRegistry       the ingress Firewall registry
     * @param clientAddressResolver the trusted client-address resolver
     * @param decisionHandler        the denied-decision handler
     */
    public LuxspecIngressFirewallFilter(
            FirewallRegistry<IngressRequest> firewallRegistry,
            ServletClientAddressResolver clientAddressResolver,
            FirewallDecisionHandler decisionHandler
    ) {
        this.firewallRegistry = Objects.requireNonNull(firewallRegistry, "firewallRegistry");
        this.clientAddressResolver = Objects.requireNonNull(clientAddressResolver, "clientAddressResolver");
        this.decisionHandler = Objects.requireNonNull(decisionHandler, "decisionHandler");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        IngressRequest ingressRequest;
        try {
            ingressRequest = ServletIngressRequestAdapter.adapt(request, clientAddressResolver);
        } catch (ServletIngressRequestAdapter.InvalidRequestPathException failure) {
            decisionHandler.handle(
                    request,
                    response,
                    FirewallDecision.deny(SecurityErrorCode.FIREWALL_PATH_DENIED)
            );
            return;
        } catch (ServletIngressRequestAdapter.InvalidClientAddressException failure) {
            decisionHandler.handle(request, response, FirewallDecision.deny(SecurityErrorCode.FIREWALL_CLIENT_ADDRESS_DENIED));
            return;
        }
        FirewallDecision decision = firewallRegistry.filter(ingressRequest);
        if (!decision.passed()) {
            decisionHandler.handle(request, response, decision);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
