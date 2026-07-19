package com.lamprism.luxspec.config;

import java.util.List;
import java.util.Objects;

/**
 * Resolves a configuration value from layers ordered from highest to lowest priority.
 *
 * @author RollW
 */
public final class LayeredConfigReader implements ConfigReader {
    private final List<ConfigLayer> layers;

    public LayeredConfigReader(List<ConfigLayer> layers) {
        this.layers = List.copyOf(layers);
    }

    @Override
    public <T> ResolvedConfig<T> get(ConfigSpec<T> spec) {
        Objects.requireNonNull(spec, "spec");
        for (ConfigLayer layer : layers) {
            ConfigSource source = layer.getSource();
            if (!ConfigSourceConstraints.matches(source, spec)) {
                continue;
            }
            ConfigEntry entry = Objects.requireNonNull(source.get(spec.getKey()), "source entry");
            if (entry.getState() == ConfigEntry.State.ABSENT) {
                continue;
            }
            if (entry.getState() == ConfigEntry.State.MASKED) {
                return ResolvedConfig.masked();
            }
            if (entry.getState() == ConfigEntry.State.INVALID) {
                throw new ConfigResolutionException(spec.getKey(), source.getId(), null);
            }
            return decode(spec, source, entry);
        }
        return spec.getDefaultValue().map(ResolvedConfig::defaultValue).orElseGet(ResolvedConfig::absent);
    }

    private <T> ResolvedConfig<T> decode(ConfigSpec<T> spec, ConfigSource source, ConfigEntry entry) {
        try {
            return ResolvedConfig.source(spec.getCodec().decode(entry.requireRawValue()), source.getId());
        } catch (RuntimeException exception) {
            throw new ConfigResolutionException(spec.getKey(), source.getId(), exception);
        }
    }
}
