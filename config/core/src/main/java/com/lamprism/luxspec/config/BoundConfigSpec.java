package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents one validated concrete binding of a template configuration definition.
 *
 * @param <T> the typed value
 * @author RollW
 */
public final class BoundConfigSpec<T> implements ConfigSpec<T> {
    private final TemplateConfigSpec<T> template;
    private final ConfigKey key;
    private final Map<String, String> arguments;

    BoundConfigSpec(TemplateConfigSpec<T> template, ConfigKey key, Map<String, String> arguments) {
        this.template = Objects.requireNonNull(template, "template");
        this.key = Objects.requireNonNull(key, "key");
        this.arguments = Map.copyOf(arguments);
    }

    public TemplateConfigSpec<T> getTemplate() {
        return template;
    }

    public Map<String, String> getArguments() {
        return arguments;
    }

    @Override
    public ConfigKey getKey() {
        return key;
    }

    @Override
    public ConfigCodec<T> getCodec() {
        return template.getCodec();
    }

    @Override
    public Optional<T> getDefaultValue() {
        return template.getDefaultValue();
    }

    @Override
    public boolean isSensitive() {
        return template.isSensitive();
    }

    @Override
    public Set<ConfigSourceCapability> getRequiredSourceCapabilities() {
        return template.getRequiredSourceCapabilities();
    }

    @Override
    public Map<String, String> getRequiredSourceAttributes() {
        return template.getRequiredSourceAttributes();
    }

    @Override
    public Optional<ConfigSourceId> getRequiredSourceId() {
        return template.getRequiredSourceId();
    }
}
