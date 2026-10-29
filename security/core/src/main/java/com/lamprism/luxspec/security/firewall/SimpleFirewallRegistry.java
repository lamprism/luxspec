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
import com.lamprism.luxspec.failure.FailureHandler;
import com.lamprism.luxspec.security.SecurityErrorCode;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory firewall registry with atomic registration snapshots.
 *
 * <p>Registry edits affect subsequent requests. A request already using a snapshot continues with
 * that snapshot, so it cannot observe a partially edited firewall list.</p>
 *
 * @param <R> the request type filtered by the registry
 *
 * @author RollW
 */
public final class SimpleFirewallRegistry<R extends FirewallRequest> implements FirewallRegistry<R> {
    private final AtomicReference<List<Firewall<? super R>>> firewalls;
    private final EventPublisher eventPublisher;
    private final Clock clock;
    private final FirewallDecisionPolicy decisionPolicy;
    private final FailureHandler<Event> eventFailureHandler;

    /**
     * Creates a registry with the supplied firewalls and no-op event publication.
     *
     * @param firewalls the initial firewalls
     */
    public SimpleFirewallRegistry(List<? extends Firewall<? super R>> firewalls) {
        this(firewalls, event -> {
        }, Clock.systemUTC());
    }

    /**
     * Creates a registry with an explicit event publisher and the UTC clock.
     *
     * @param firewalls      the initial firewalls
     * @param eventPublisher best-effort event publisher
     */
    public SimpleFirewallRegistry(
            List<? extends Firewall<? super R>> firewalls,
            EventPublisher eventPublisher
    ) {
        this(firewalls, eventPublisher, Clock.systemUTC());
    }

    /**
     * Creates a registry with explicit event and time dependencies.
     *
     * @param firewalls      the initial firewalls
     * @param eventPublisher best-effort event publisher
     * @param clock          event timestamp clock
     */
    public SimpleFirewallRegistry(
            List<? extends Firewall<? super R>> firewalls,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this(firewalls, eventPublisher, clock, FirewallDecisionPolicy.firstDecision(), (event, failure) -> {
        });
    }

    /**
     * Creates a registry with explicit decision and event failure policies.
     *
     * @param firewalls           the initial firewalls
     * @param eventPublisher      the event publisher
     * @param clock               the event timestamp clock
     * @param decisionPolicy      the decision combination policy
     * @param eventFailureHandler the observer failure policy
     */
    public SimpleFirewallRegistry(
            List<? extends Firewall<? super R>> firewalls,
            EventPublisher eventPublisher,
            Clock clock,
            FirewallDecisionPolicy decisionPolicy,
            FailureHandler<Event> eventFailureHandler
    ) {
        this.firewalls = new AtomicReference<>(sortFirewalls(firewalls));
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.decisionPolicy = Objects.requireNonNull(decisionPolicy, "decisionPolicy");
        this.eventFailureHandler = Objects.requireNonNull(eventFailureHandler, "eventFailureHandler");
    }

    @Override
    public List<Firewall<? super R>> getFirewalls() {
        return firewalls.get();
    }

    @Override
    public void addFirewall(Firewall<? super R> firewall) {
        Firewall<? super R> nonNullFirewall = Objects.requireNonNull(firewall, "firewall");
        while (true) {
            List<Firewall<? super R>> current = firewalls.get();
            if (current.contains(nonNullFirewall)) {
                throw new IllegalArgumentException("firewall is already registered");
            }
            List<Firewall<? super R>> replacement = new ArrayList<>(current);
            replacement.add(nonNullFirewall);
            List<Firewall<? super R>> ordered = sortFirewalls(replacement);
            if (firewalls.compareAndSet(current, ordered)) {
                return;
            }
        }
    }

    @Override
    public boolean removeFirewall(Firewall<? super R> firewall) {
        Firewall<? super R> nonNullFirewall = Objects.requireNonNull(firewall, "firewall");
        while (true) {
            List<Firewall<? super R>> current = firewalls.get();
            if (!current.contains(nonNullFirewall)) {
                return false;
            }
            List<Firewall<? super R>> replacement = new ArrayList<>(current);
            replacement.remove(nonNullFirewall);
            List<Firewall<? super R>> immutableReplacement = List.copyOf(replacement);
            if (firewalls.compareAndSet(current, immutableReplacement)) {
                return true;
            }
        }
    }

    @Override
    public FirewallDecision filter(R request) {
        R nonNullRequest = Objects.requireNonNull(request, "request");
        FirewallDecision combined = FirewallDecision.neutral();
        for (Firewall<? super R> firewall : firewalls.get()) {
            FirewallDecision decision = evaluate(firewall, nonNullRequest);
            combined = Objects.requireNonNull(decisionPolicy.combine(combined, decision), "combined decision");
            if (decisionPolicy.isTerminal(combined)) {
                return combined;
            }
        }
        return combined;
    }

    @Override
    public void clearContext(R request) {
        R nonNullRequest = Objects.requireNonNull(request, "request");
        for (Firewall<? super R> firewall : firewalls.get()) {
            firewall.clearContext(nonNullRequest);
        }
    }

    @Override
    public void clearAllContext() {
        for (Firewall<? super R> firewall : firewalls.get()) {
            firewall.clearAllContext();
        }
    }

    private FirewallDecision evaluate(Firewall<? super R> firewall, R request) {
        FirewallDecision decision;
        try {
            decision = Objects.requireNonNull(firewall.verifyRequest(request), "firewall decision");
        } catch (RuntimeException exception) {
            publish(new FirewallRuleFailureEvent(
                    firewall.getClass().getName(),
                    SecurityErrorCode.FIREWALL_RULE_FAILURE,
                    clock.instant()
            ));
            return FirewallDecision.deny(SecurityErrorCode.FIREWALL_RULE_FAILURE);
        }
        if (decision.getCase() == FirewallDecision.Case.DENY) {
            publish(new FirewallRuleDeniedEvent(
                    firewall.getClass().getName(),
                    decision.getReasonCode(),
                    decision.getRetryAfter().orElse(null),
                    clock.instant()
            ));
        }
        return decision;
    }

    private void publish(Event event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException failure) {
            eventFailureHandler.onFailure(event, failure);
        }
    }

    private static <R extends FirewallRequest> List<Firewall<? super R>> sortFirewalls(
            List<? extends Firewall<? super R>> firewalls
    ) {
        Objects.requireNonNull(firewalls, "firewalls");
        List<Firewall<? super R>> copied = new ArrayList<>();
        for (Firewall<? super R> firewall : firewalls) {
            copied.add(Objects.requireNonNull(firewall, "firewall"));
        }
        copied.sort((left, right) -> Integer.compare(right.getPriority(), left.getPriority()));
        return List.copyOf(copied);
    }
}
