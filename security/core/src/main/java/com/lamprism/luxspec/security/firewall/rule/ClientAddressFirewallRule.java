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

package com.lamprism.luxspec.security.firewall.rule;

import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.firewall.FirewallDecision;
import com.lamprism.luxspec.security.firewall.FirewallRule;
import com.lamprism.luxspec.security.firewall.IngressRequest;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Allows or denies requests by their trusted normalized client address.
 *
 * @author RollW
 */
public class ClientAddressFirewallRule implements FirewallRule<IngressRequest> {
    private final Set<String> addresses;
    private final boolean allowMatch;

    /**
     * Creates a client-address rule for subclass customization.
     *
     * @param addresses  the normalized client addresses to match
     * @param allowMatch whether matching addresses pass the rule
     */
    protected ClientAddressFirewallRule(Collection<String> addresses, boolean allowMatch) {
        this.addresses = FirewallRuleSupport.copyTextValues(addresses, "addresses");
        this.allowMatch = allowMatch;
    }

    /**
     * Creates a client-address allow-list rule.
     *
     * @param addresses the allowed normalized client addresses
     * @return the allow-list rule
     */
    public static ClientAddressFirewallRule allowOnly(Collection<String> addresses) {
        return new ClientAddressFirewallRule(addresses, true);
    }

    /**
     * Creates a client-address deny-list rule.
     *
     * @param addresses the denied normalized client addresses
     * @return the deny-list rule
     */
    public static ClientAddressFirewallRule deny(Collection<String> addresses) {
        return new ClientAddressFirewallRule(addresses, false);
    }

    @Override
    public FirewallDecision evaluate(IngressRequest request) {
        IngressRequest nonNullRequest = Objects.requireNonNull(request, "request");
        return FirewallRuleSupport.decide(
                addresses.contains(nonNullRequest.getClientAddress()),
                allowMatch,
                SecurityErrorCode.FIREWALL_CLIENT_ADDRESS_DENIED
        );
    }
}
