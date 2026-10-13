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

package com.lamprism.luxspec.security.firewall.filtertable;

import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.firewall.Firewall;
import com.lamprism.luxspec.security.firewall.FirewallDecision;
import com.lamprism.luxspec.security.firewall.FirewallRequest;

import java.time.Clock;
import java.util.Objects;

/**
 * Adapts a mutable filter table to the three-state firewall registry contract.
 *
 * @author RollW
 */
public final class FilterTableFirewall implements Firewall<FirewallRequest> {
    private final FilterTable filterTable;
    private final int priority;
    private final Clock clock;

    /**
     * Creates a filter-table firewall with default priority and UTC time.
     *
     * @param filterTable the dynamic filter table
     */
    public FilterTableFirewall(FilterTable filterTable) {
        this(filterTable, 0, Clock.systemUTC());
    }

    /**
     * Creates a filter-table firewall with explicit registry priority and clock.
     *
     * @param filterTable the dynamic filter table
     * @param priority    registry priority
     * @param clock       expiration clock
     */
    public FilterTableFirewall(FilterTable filterTable, int priority, Clock clock) {
        this.filterTable = Objects.requireNonNull(filterTable, "filterTable");
        this.priority = priority;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public FirewallDecision verifyRequest(FirewallRequest request) {
        FirewallRequest nonNullRequest = Objects.requireNonNull(request, "request");
        FilterEntry match = filterTable.snapshot().findMatch(nonNullRequest, clock.instant());
        if (match == null) {
            return FirewallDecision.neutral();
        }
        if (match.mode() == FilterMode.ALLOW) {
            return FirewallDecision.allow();
        }
        return FirewallDecision.deny(denialCode(match.target()));
    }

    @Override
    public void clearAllContext() {
        filterTable.clear();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    private static SecurityErrorCode denialCode(FilterTarget target) {
        if (target instanceof FilterTarget.SubjectTarget) {
            return SecurityErrorCode.FIREWALL_SUBJECT_DENIED;
        }
        return SecurityErrorCode.FIREWALL_CLIENT_ADDRESS_DENIED;
    }
}
