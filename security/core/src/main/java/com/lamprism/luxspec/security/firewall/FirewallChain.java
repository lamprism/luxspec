package com.lamprism.luxspec.security.firewall;

import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.event.EventPublisher;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates immutable ordered firewall rules where every rule must pass.
 *
 * @param <R> the request-fact type evaluated by this chain
 * @author RollW
 */
public final class FirewallChain<R> {
    private final List<FirewallRule<R>> rules;
    private final EventPublisher eventPublisher;

    /**
     * Creates a chain whose rule order is the supplied immutable order.
     *
     * @param rules firewall rules to evaluate
     */
    public FirewallChain(List<? extends FirewallRule<R>> rules) {
        this(rules, event -> { });
    }

    /**
     * Creates a chain with safe failure event publication.
     *
     * @param rules firewall rules to evaluate
     * @param eventPublisher the publisher notified when a rule fails unexpectedly
     */
    public FirewallChain(List<? extends FirewallRule<R>> rules, EventPublisher eventPublisher) {
        this.rules = List.copyOf(rules);
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    /**
     * Evaluates every rule until the first denial.
     *
     * @param request immutable request facts
     * @return the first denial or a shared pass decision
     */
    public FirewallDecision evaluate(R request) {
        Objects.requireNonNull(request, "request");
        for (FirewallRule<R> rule : rules) {
            FirewallDecision decision = evaluate(rule, request);
            if (!decision.passed()) {
                return decision;
            }
        }
        return FirewallDecision.pass();
    }

    private FirewallDecision evaluate(FirewallRule<R> rule, R request) {
        try {
            return Objects.requireNonNull(rule.evaluate(request), "firewall decision");
        } catch (RuntimeException exception) {
            eventPublisher.publish(new FirewallRuleFailureEvent(rule.getClass().getName()));
            return FirewallDecision.deny(SecurityErrorCode.FIREWALL_RULE_FAILURE);
        }
    }
}
