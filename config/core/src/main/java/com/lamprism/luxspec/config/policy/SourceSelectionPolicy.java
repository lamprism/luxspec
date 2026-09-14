package com.lamprism.luxspec.config.policy;

import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Map;
import java.util.Objects;

final class SourceSelectionPolicy implements ConfigPolicy {
    private final Map<String, String> attributes;
    private final ConfigSourceSelector selector;

    SourceSelectionPolicy(
            Map<String, String> attributes,
            ConfigSourceSelector selector
    ) {
        this.attributes = validateAttributes(attributes);
        this.selector = Objects.requireNonNull(selector, "selector");
    }

    @Override
    public ConfigPolicyDecision evaluate(ConfigPolicyContext context) {
        ConfigPolicyContext nonNullContext = Objects.requireNonNull(context, "context");
        ConfigSourceId sourceId = nonNullContext.getSourceId();
        if (sourceId == null) {
            return ConfigPolicyDecision.ALLOW;
        }
        if (matches(
                sourceId,
                nonNullContext.getSourceAttributes()
        )) {
            return ConfigPolicyDecision.ALLOW;
        }
        return isResolution(nonNullContext.getOperation())
                ? ConfigPolicyDecision.SKIP
                : ConfigPolicyDecision.DENY;
    }

    private static boolean isResolution(ConfigPolicyOperation operation) {
        return operation == ConfigPolicyOperation.RESOLVE_SOURCE;
    }

    private boolean matches(
            ConfigSourceId sourceId,
            Map<String, String> sourceAttributes
    ) {
        ConfigSourceId nonNullSourceId = Objects.requireNonNull(sourceId, "sourceId");
        Map<String, String> nonNullAttributes = Objects.requireNonNull(sourceAttributes, "sourceAttributes");
        return selector.matches(nonNullSourceId)
                && nonNullAttributes.entrySet().containsAll(attributes.entrySet());
    }

    private static Map<String, String> validateAttributes(Map<String, String> attributes) {
        Map<String, String> values = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Source attribute names must not be blank");
            }
        }
        return values;
    }
}
