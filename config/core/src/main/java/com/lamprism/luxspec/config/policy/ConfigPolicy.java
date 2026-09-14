package com.lamprism.luxspec.config.policy;

import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates one operation-aware configuration constraint.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigPolicy {
    /**
     * Evaluates the policy against immutable operation metadata.
     *
     * @param context the policy context
     * @return the explicit policy decision
     */
    ConfigPolicyDecision evaluate(ConfigPolicyContext context);

    /**
     * Composes this policy with another policy.
     *
     * @param other the additional policy
     * @return the deterministic composite policy
     */
    default ConfigPolicy and(ConfigPolicy other) {
        return allOf(this, Objects.requireNonNull(other, "other"));
    }

    /**
     * Returns an allow-all policy.
     *
     * @return the allow-all policy
     */
    static ConfigPolicy allow() {
        return context -> ConfigPolicyDecision.ALLOW;
    }

    /**
     * Composes policies using deterministic decision precedence.
     *
     * @param policies the policies to evaluate in declaration order
     * @return the composite policy
     */
    static ConfigPolicy allOf(Iterable<? extends ConfigPolicy> policies) {
        List<ConfigPolicy> values = new ArrayList<>();
        for (ConfigPolicy policy : Objects.requireNonNull(policies, "policies")) {
            values.add(Objects.requireNonNull(policy, "policy"));
        }
        if (values.isEmpty()) {
            return allow();
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
    static ConfigPolicy allOf(ConfigPolicy first, ConfigPolicy second, ConfigPolicy... additional) {
        List<ConfigPolicy> values = new ArrayList<>();
        values.add(Objects.requireNonNull(first, "first"));
        values.add(Objects.requireNonNull(second, "second"));
        for (ConfigPolicy policy : Objects.requireNonNull(additional, "additional")) {
            values.add(Objects.requireNonNull(policy, "policy"));
        }
        return allOf(values);
    }

    /**
     * Creates a source-selection policy from source metadata.
     *
     * @param attributes required source attributes
     * @param selector   selected source IDs
     * @return the source-selection policy
     */
    static ConfigPolicy sourceSelection(Map<String, String> attributes, ConfigSourceSelector selector) {
        return new SourceSelectionPolicy(attributes, selector);
    }

    /**
     * Creates a source-selection policy that accepts every configured source.
     *
     * @return the default source-selection policy
     */
    static ConfigPolicy anySource() {
        return sourceSelection(Map.of(), ConfigSourceSelector.any());
    }

    /**
     * Creates a policy that accepts only sources from the required lifecycle scope.
     *
     * @param scope the required source scope
     * @return the scope policy
     */
    static ConfigPolicy sourceScope(ConfigSourceScope scope) {
        ConfigSourceScope requiredScope = Objects.requireNonNull(scope, "scope");
        return context -> {
            ConfigSourceScope sourceScope = context.getSourceScope();
            if (sourceScope == null || requiredScope == sourceScope) {
                return ConfigPolicyDecision.ALLOW;
            }
            return context.getOperation() == ConfigPolicyOperation.RESOLVE_SOURCE
                    ? ConfigPolicyDecision.SKIP
                    : ConfigPolicyDecision.DENY;
        };
    }

    /**
     * Creates a policy that rejects all typed mutations.
     *
     * @return the read-only policy
     */
    static ConfigPolicy readOnly() {
        return context -> switch (context.getOperation()) {
            case SET, REMOVE -> ConfigPolicyDecision.DENY;
            default -> ConfigPolicyDecision.ALLOW;
        };
    }

    /**
     * Creates a policy that prevents use of the spec default.
     *
     * @return the no-fallback policy
     */
    static ConfigPolicy noFallback() {
        return context -> context.getOperation() == ConfigPolicyOperation.RESOLVE_FALLBACK
                ? ConfigPolicyDecision.STOP
                : ConfigPolicyDecision.ALLOW;
    }

    private static ConfigPolicyDecision combine(List<ConfigPolicy> policies, ConfigPolicyContext context) {
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
