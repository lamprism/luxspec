package com.lamprism.luxspec.config.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Factory and composition methods for common configuration policies.
 *
 * @author RollW
 */
public final class ConfigPolicies {
    private ConfigPolicies() {
    }

    /**
     * Composes policies using deterministic decision precedence.
     *
     * @param policies the policies to evaluate in declaration order
     * @return the composite policy
     */
    public static ConfigPolicy allOf(Iterable<? extends ConfigPolicy> policies) {
        List<ConfigPolicy> values = new ArrayList<>();
        for (ConfigPolicy policy : Objects.requireNonNull(policies, "policies")) {
            values.add(Objects.requireNonNull(policy, "policy"));
        }
        if (values.isEmpty()) {
            return ConfigPolicy.allow();
        }
        return context -> combine(values, context);
    }

    /**
     * Composes policies using deterministic decision precedence.
     *
     * @param first      the first policy
     * @param second     the second policy
     * @param additional additional policies
     * @return the composite policy
     */
    public static ConfigPolicy allOf(
            ConfigPolicy first,
            ConfigPolicy second,
            ConfigPolicy... additional
    ) {
        List<ConfigPolicy> values = new ArrayList<>();
        values.add(Objects.requireNonNull(first, "first"));
        values.add(Objects.requireNonNull(second, "second"));
        for (ConfigPolicy policy : Objects.requireNonNull(additional, "additional")) {
            values.add(Objects.requireNonNull(policy, "policy"));
        }
        return allOf(values);
    }

    /**
     * Creates a Source-selection policy from Source metadata.
     *
     * @param attributes required Source attributes
     * @param selector   selected Source IDs
     * @return the Source-selection policy
     */
    public static ConfigPolicy sourceSelection(
            Map<String, String> attributes,
            ConfigSourceSelector selector
    ) {
        return new SourceSelectionPolicy(attributes, selector);
    }

    /**
     * Creates a Source-selection policy that accepts every configured Source.
     *
     * @return the default Source-selection policy
     */
    public static ConfigPolicy anySource() {
        return sourceSelection(
                Map.of(),
                ConfigSourceSelector.any()
        );
    }

    /**
     * Creates a policy that rejects all typed mutations.
     *
     * @return the read-only policy
     */
    public static ConfigPolicy readOnly() {
        return context -> switch (context.getOperation()) {
            case SET, REMOVE -> ConfigPolicyDecision.DENY;
            default -> ConfigPolicyDecision.ALLOW;
        };
    }

    /**
     * Creates a policy that prevents use of the Spec default.
     *
     * @return the no-fallback policy
     */
    public static ConfigPolicy noFallback() {
        return context -> context.getOperation() == ConfigPolicyOperation.RESOLVE_FALLBACK
                ? ConfigPolicyDecision.STOP
                : ConfigPolicyDecision.ALLOW;
    }

    private static ConfigPolicyDecision combine(
            List<ConfigPolicy> policies,
            ConfigPolicyContext context
    ) {
        ConfigPolicyDecision result = ConfigPolicyDecision.ALLOW;
        for (ConfigPolicy policy : policies) {
            ConfigPolicyDecision decision = Objects.requireNonNull(
                    policy.evaluate(Objects.requireNonNull(context, "context")),
                    "policy decision"
            );
            if (priority(decision) > priority(result)) {
                result = decision;
            }
        }
        return result;
    }

    private static int priority(ConfigPolicyDecision decision) {
        return switch (decision) {
            case ALLOW -> 0;
            case SKIP -> 1;
            case STOP -> 2;
            case DENY -> 3;
        };
    }
}
