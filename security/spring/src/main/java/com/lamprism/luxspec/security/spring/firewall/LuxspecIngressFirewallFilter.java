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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Applies an ingress Firewall chain before authentication and downstream processing.
 *
 * <p>The filter is explicit and is not installed by security auto-configuration. Applications
 * choose its position relative to authentication filters.</p>
 *
 * @author RollW
 */
public class LuxspecIngressFirewallFilter extends OncePerRequestFilter {
    private final FirewallChain<IngressRequest> firewallChain;
    private final ServletIngressRequestAdapter requestAdapter;
    private final FirewallDecisionHandler decisionHandler;

    /**
     * Creates an ingress filter with Servlet address extraction and HTTP 403 denials.
     *
     * @param firewallChain the ingress Firewall chain
     */
    public LuxspecIngressFirewallFilter(FirewallChain<IngressRequest> firewallChain) {
        this(
                firewallChain,
                new ServletIngressRequestAdapter(),
                new HttpStatusFirewallDecisionHandler()
        );
    }

    /**
     * Creates an ingress filter with explicit Servlet extraction and denial handling.
     *
     * @param firewallChain   the ingress Firewall chain
     * @param requestAdapter  the Servlet ingress adapter
     * @param decisionHandler the denied-decision handler
     */
    public LuxspecIngressFirewallFilter(
            FirewallChain<IngressRequest> firewallChain,
            ServletIngressRequestAdapter requestAdapter,
            FirewallDecisionHandler decisionHandler
    ) {
        this.firewallChain = Objects.requireNonNull(firewallChain, "firewallChain");
        this.requestAdapter = Objects.requireNonNull(requestAdapter, "requestAdapter");
        this.decisionHandler = Objects.requireNonNull(decisionHandler, "decisionHandler");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        FirewallDecision decision = firewallChain.evaluate(requestAdapter.adapt(request));
        if (!decision.passed()) {
            decisionHandler.handle(request, response, decision);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
