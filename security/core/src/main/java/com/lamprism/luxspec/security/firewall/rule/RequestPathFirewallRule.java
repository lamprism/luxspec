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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Allows or denies requests by exact paths or path prefixes.
 *
 * @author RollW
 */
public class RequestPathFirewallRule implements FirewallRule<IngressRequest> {
    private enum MatchType {
        EXACT,
        PREFIX
    }

    private final Set<String> paths;
    private final MatchType matchType;
    private final boolean allowMatch;

    /**
     * Creates a path rule for subclass customization.
     *
     * @param paths       the paths to match
     * @param prefixMatch whether each configured path is a path prefix
     * @param allowMatch  whether matching paths pass the rule
     */
    protected RequestPathFirewallRule(Collection<String> paths, boolean prefixMatch, boolean allowMatch) {
        MatchType nonNullMatchType = prefixMatch ? MatchType.PREFIX : MatchType.EXACT;
        this.paths = copyPaths(paths, nonNullMatchType);
        this.matchType = nonNullMatchType;
        this.allowMatch = allowMatch;
    }

    /**
     * Creates an exact-path allow-list rule.
     *
     * @param paths the allowed request paths
     * @return the allow-list rule
     */
    public static RequestPathFirewallRule allowExact(Collection<String> paths) {
        return new RequestPathFirewallRule(paths, false, true);
    }

    /**
     * Creates an exact-path deny-list rule.
     *
     * @param paths the denied request paths
     * @return the deny-list rule
     */
    public static RequestPathFirewallRule denyExact(Collection<String> paths) {
        return new RequestPathFirewallRule(paths, false, false);
    }

    /**
     * Creates a path-prefix allow-list rule.
     *
     * @param prefixes the allowed path prefixes
     * @return the allow-list rule
     */
    public static RequestPathFirewallRule allowPrefixes(Collection<String> prefixes) {
        return new RequestPathFirewallRule(prefixes, true, true);
    }

    /**
     * Creates a path-prefix deny-list rule.
     *
     * @param prefixes the denied path prefixes
     * @return the deny-list rule
     */
    public static RequestPathFirewallRule denyPrefixes(Collection<String> prefixes) {
        return new RequestPathFirewallRule(prefixes, true, false);
    }

    @Override
    public FirewallDecision evaluate(IngressRequest request) {
        String requestPath = Objects.requireNonNull(request, "request").getPath();
        boolean matched = paths.stream().anyMatch(path -> matches(path, requestPath));
        return FirewallRuleSupport.decide(
                matched,
                allowMatch,
                SecurityErrorCode.FIREWALL_PATH_DENIED
        );
    }

    private boolean matches(String configuredPath, String requestPath) {
        if (matchType == MatchType.EXACT) {
            return configuredPath.equals(requestPath);
        }
        if (configuredPath.equals("/")) {
            return requestPath.startsWith("/");
        }
        return configuredPath.equals(requestPath) || requestPath.startsWith(configuredPath + "/");
    }

    private static Set<String> copyPaths(Collection<String> paths, MatchType matchType) {
        Set<String> configuredPaths = FirewallRuleSupport.copyTextValues(paths, "paths");
        Set<String> result = new LinkedHashSet<>();
        for (String path : configuredPaths) {
            requirePath(path);
            result.add(matchType == MatchType.PREFIX ? normalizePrefix(path) : path);
        }
        if (result.size() != configuredPaths.size()) {
            throw new IllegalArgumentException("paths must not contain equivalent values");
        }
        return Set.copyOf(result);
    }

    private static void requirePath(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("paths must start with '/'");
        }
    }

    private static String normalizePrefix(String path) {
        int end = path.length();
        while (end > 1 && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(0, end);
    }
}
