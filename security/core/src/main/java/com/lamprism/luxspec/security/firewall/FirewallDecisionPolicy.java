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

/**
 * Combines ordered firewall decisions and determines when evaluation can stop.
 *
 * @author RollW
 */
public interface FirewallDecisionPolicy {
    /**
     * Returns the first non-neutral decision in priority order.
     *
     * @return the short-circuit policy
     */
    static FirewallDecisionPolicy firstDecision() {
        return new FirewallDecisionPolicy() {
            @Override
            public FirewallDecision combine(FirewallDecision current, FirewallDecision next) {
                return current.getCase() == FirewallDecision.Case.NEUTRAL ? next : current;
            }

            @Override
            public boolean isTerminal(FirewallDecision decision) {
                return decision.getCase() != FirewallDecision.Case.NEUTRAL;
            }
        };
    }

    /**
     * Evaluates until a denial is found; an allow is retained if no rule denies.
     *
     * @return the denial-priority policy
     */
    static FirewallDecisionPolicy denyOverrides() {
        return new FirewallDecisionPolicy() {
            @Override
            public FirewallDecision combine(FirewallDecision current, FirewallDecision next) {
                if (next.getCase() == FirewallDecision.Case.DENY) {
                    return next;
                }
                return current.getCase() == FirewallDecision.Case.NEUTRAL ? next : current;
            }

            @Override
            public boolean isTerminal(FirewallDecision decision) {
                return decision.getCase() == FirewallDecision.Case.DENY;
            }
        };
    }

    /**
     * Combines the aggregate so far with the next rule decision.
     *
     * @param current the current aggregate, initially neutral
     * @param next    the next rule decision
     * @return the new aggregate
     */
    FirewallDecision combine(FirewallDecision current, FirewallDecision next);

    /**
     * Reports whether later rules cannot change the aggregate.
     *
     * @param decision the current aggregate
     * @return whether evaluation can stop
     */
    boolean isTerminal(FirewallDecision decision);
}
