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
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.security.SecurityErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates immutable ordered firewall rules where every rule must pass.
 *
 * <p>Denial and rule-failure events are best-effort notifications. A publisher runtime failure
 * never replaces the evaluated firewall decision.</p>
 *
 * @param <R> the request-fact type evaluated by this chain
 * @author RollW
 */
public class FirewallChain<R> {
    private final List<FirewallRule<? super R>> rules;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Creates a chain whose rule order is the supplied immutable order.
     *
     * @param rules firewall rules to evaluate; each rule may accept {@code R} or a supertype of
     *              {@code R}
     */
    public FirewallChain(List<? extends FirewallRule<? super R>> rules) {
        this(rules, event -> {
        }, Clock.systemUTC());
    }

    /**
     * Creates a chain with best-effort failure event publication.
     *
     * @param rules          firewall rules to evaluate; each rule may accept {@code R} or a
     *                       supertype of {@code R}
     * @param eventPublisher the publisher notified for denials and unexpected failures
     */
    public FirewallChain(List<? extends FirewallRule<? super R>> rules, EventPublisher eventPublisher) {
        this(rules, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates a chain with denial and failure event publication using an explicit clock.
     *
     * @param rules          firewall rules to evaluate; each rule may accept {@code R} or a
     *                       supertype of {@code R}
     * @param eventPublisher the publisher notified for denials and unexpected failures
     * @param clock          the event timestamp clock
     */
    public FirewallChain(
            List<? extends FirewallRule<? super R>> rules,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.rules = List.copyOf(rules);
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Evaluates every rule until the first denial.
     *
     * @param request immutable request facts
     * @return the first denial or a shared pass decision
     */
    public FirewallDecision evaluate(R request) {
        Objects.requireNonNull(request, "request");
        for (FirewallRule<? super R> rule : rules) {
            FirewallDecision decision = evaluate(rule, request);
            if (!decision.passed()) {
                return decision;
            }
        }
        return FirewallDecision.pass();
    }

    private FirewallDecision evaluate(FirewallRule<? super R> rule, R request) {
        FirewallDecision decision;
        try {
            decision = Objects.requireNonNull(rule.evaluate(request), "firewall decision");
        } catch (RuntimeException exception) {
            publishBestEffort(new FirewallRuleFailureEvent(
                    rule.getClass().getName(),
                    SecurityErrorCode.FIREWALL_RULE_FAILURE,
                    clock.instant()
            ));
            return FirewallDecision.deny(SecurityErrorCode.FIREWALL_RULE_FAILURE);
        }
        if (!decision.passed()) {
            Instant occurredAt = clock.instant();
            publishBestEffort(new FirewallRuleDeniedEvent(
                    rule.getClass().getName(),
                    decision.getReasonCode(),
                    decision.getRetryAfter().orElse(null),
                    occurredAt
            ));
        }
        return decision;
    }

    private void publishBestEffort(Event event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException ignored) {
            // Firewall decisions remain authoritative when an observer fails.
        }
    }
}
