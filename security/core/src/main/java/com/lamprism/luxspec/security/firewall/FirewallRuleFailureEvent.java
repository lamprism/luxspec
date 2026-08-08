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

import com.lamprism.luxspec.event.Event;

import java.util.Objects;

/**
 * Reports a firewall rule failure that was converted to a default denial.
 *
 * @author RollW
 */
public final class FirewallRuleFailureEvent implements Event {
    private final String ruleType;

    /**
     * Creates safe failure metadata without retaining the request or throwable.
     *
     * @param ruleType the failing rule implementation type name
     */
    public FirewallRuleFailureEvent(String ruleType) {
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType");
    }

    /**
     * Returns the failing rule implementation type name.
     *
     * @return the rule type name
     */
    public String getRuleType() {
        return ruleType;
    }
}
