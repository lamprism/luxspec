package com.lamprism.luxspec.security.firewall;

/**
 * Evaluates one framework-independent firewall policy.
 *
 * @author RollW
 */
public interface FirewallRule<R> {
    /**
     * Evaluates one request at the rule chain's declared stage.
     *
     * @param request immutable request facts
     * @return pass or deny decision
     */
    FirewallDecision evaluate(R request);
}
