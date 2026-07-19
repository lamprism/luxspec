package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Objects;

final class ConfigSourceConstraints {
    private ConfigSourceConstraints() {
    }

    static Map<String, String> validateAttributes(Map<String, String> attributes) {
        Map<String, String> values = Map.copyOf(Objects.requireNonNull(attributes, "requiredSourceAttributes"));
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Source attribute names must not be blank");
            }
        }
        return values;
    }

    static boolean matches(ConfigSource source, ConfigSpec<?> spec) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(spec, "spec");
        ConfigSourceId sourceId = Objects.requireNonNull(source.getId(), "source ID");
        if (spec.getRequiredSourceId().isPresent()
                && !spec.getRequiredSourceId().orElseThrow().equals(sourceId)) {
            return false;
        }
        if (!Objects.requireNonNull(source.getCapabilities(), "source capabilities")
                .containsAll(spec.getRequiredSourceCapabilities())) {
            return false;
        }
        Map<String, String> sourceAttributes = Map.copyOf(
                Objects.requireNonNull(source.getAttributes(), "source attributes")
        );
        return sourceAttributes.entrySet().containsAll(spec.getRequiredSourceAttributes().entrySet());
    }
}
