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

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.firewall.AuthenticatedRequest;
import com.lamprism.luxspec.security.firewall.FirewallChain;
import com.lamprism.luxspec.security.firewall.FirewallDecision;
import com.lamprism.luxspec.security.firewall.IngressRequest;
import com.lamprism.luxspec.security.spring.authentication.LuxspecSpringAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Applies an authenticated Firewall chain when the current Spring authentication is Luxspec-backed.
 *
 * <p>Install this filter after the authentication filter so the Luxspec SecurityContext is
 * available while the chain evaluates the request.</p>
 *
 * @author RollW
 */
public class LuxspecAuthenticatedFirewallFilter extends OncePerRequestFilter {
    private final FirewallChain<AuthenticatedRequest> firewallChain;
    private final ServletClientAddressResolver clientAddressResolver;
    private final FirewallDecisionHandler decisionHandler;

    /**
     * Creates an authenticated filter with Servlet remote-address resolution and HTTP 403 denials.
     *
     * @param firewallChain the authenticated Firewall chain
     */
    public LuxspecAuthenticatedFirewallFilter(FirewallChain<AuthenticatedRequest> firewallChain) {
        this(
                firewallChain,
                ServletClientAddressResolver.remoteAddress(),
                new HttpStatusFirewallDecisionHandler()
        );
    }

    /**
     * Creates an authenticated filter with explicit client-address resolution and denial handling.
     *
     * @param firewallChain          the authenticated Firewall chain
     * @param clientAddressResolver the trusted client-address resolver
     * @param decisionHandler        the denied-decision handler
     */
    public LuxspecAuthenticatedFirewallFilter(
            FirewallChain<AuthenticatedRequest> firewallChain,
            ServletClientAddressResolver clientAddressResolver,
            FirewallDecisionHandler decisionHandler
    ) {
        this.firewallChain = Objects.requireNonNull(firewallChain, "firewallChain");
        this.clientAddressResolver = Objects.requireNonNull(clientAddressResolver, "clientAddressResolver");
        this.decisionHandler = Objects.requireNonNull(decisionHandler, "decisionHandler");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        org.springframework.security.core.Authentication springAuthentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (!(springAuthentication instanceof LuxspecSpringAuthentication luxspecAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }
        IngressRequest ingressRequest = ServletIngressRequestAdapter.adapt(request, clientAddressResolver);
        Authentication authentication = luxspecAuthentication.getLuxspecAuthentication();
        FirewallDecision decision = firewallChain.evaluate(new AuthenticatedRequest(ingressRequest, authentication));
        if (!decision.passed()) {
            decisionHandler.handle(request, response, decision);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
