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
import com.lamprism.luxspec.security.firewall.FirewallRequest;
import com.lamprism.luxspec.security.firewall.FirewallRule;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Allows or denies requests by their HTTP method.
 *
 * @author RollW
 */
public class RequestMethodFirewallRule implements FirewallRule<FirewallRequest> {
    private final Set<String> methods;
    private final boolean allowMatch;

    /**
     * Creates an HTTP method rule for subclass customization.
     *
     * @param methods    the HTTP methods to match
     * @param allowMatch whether matching methods pass the rule
     */
    protected RequestMethodFirewallRule(Collection<String> methods, boolean allowMatch) {
        Set<String> normalizedMethods = new LinkedHashSet<>();
        for (String method : FirewallRuleSupport.copyTextValues(methods, "methods")) {
            if (!normalizedMethods.add(method.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("methods must not contain equivalent values");
            }
        }
        this.methods = Set.copyOf(normalizedMethods);
        this.allowMatch = allowMatch;
    }

    /**
     * Creates a rule that allows only the supplied HTTP methods.
     *
     * @param methods the allowed HTTP methods
     * @return the allow-list rule
     */
    public static RequestMethodFirewallRule allowOnly(Collection<String> methods) {
        return new RequestMethodFirewallRule(methods, true);
    }

    /**
     * Creates a rule that denies the supplied HTTP methods.
     *
     * @param methods the denied HTTP methods
     * @return the deny-list rule
     */
    public static RequestMethodFirewallRule deny(Collection<String> methods) {
        return new RequestMethodFirewallRule(methods, false);
    }

    @Override
    public FirewallDecision evaluate(FirewallRequest request) {
        String method = Objects.requireNonNull(request, "request").getMethod().toUpperCase(Locale.ROOT);
        return FirewallRuleSupport.decide(
                methods.contains(method),
                allowMatch,
                SecurityErrorCode.FIREWALL_METHOD_DENIED
        );
    }
}
