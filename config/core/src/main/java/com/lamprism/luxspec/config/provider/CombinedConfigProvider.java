package com.lamprism.luxspec.config.provider;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigProvider;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.ConfigWriter;
import com.lamprism.luxspec.config.resolution.LayeredConfigValue;
import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Combines ordered Provider readers into one layered result and delegates mutations to one writer.
 *
 * <p>Readers are ordered from highest to lowest precedence. The writer is explicit so composing
 * readers never accidentally selects a writable Source by iteration order.</p>
 *
 * @author RollW
 */
public final class CombinedConfigProvider implements ConfigProvider {
    private final List<ConfigReader> readers;
    private final ConfigWriter writer;

    /**
     * Creates a combined Provider with an explicit writer.
     *
     * @param readers the ordered Provider readers
     * @param writer  the mutation delegate
     */
    public CombinedConfigProvider(Iterable<? extends ConfigReader> readers, ConfigWriter writer) {
        List<ConfigReader> values = new ArrayList<>();
        for (ConfigReader reader : Objects.requireNonNull(readers, "readers")) {
            values.add(Objects.requireNonNull(reader, "reader"));
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one configuration reader is required");
        }
        this.readers = List.copyOf(values);
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        List<ConfigValue<T>> layers = new ArrayList<>();
        for (ConfigReader reader : readers) {
            layers.add(reader.get(nonNullBinding));
        }
        return LayeredConfigValue.of(layers);
    }

    @Override
    public <T> void set(ConfigBinding<T> binding, T value) {
        writer.set(binding, value);
    }

    @Override
    public <T> void set(ConfigSourceId sourceId, ConfigBinding<T> binding, T value) {
        writer.set(sourceId, binding, value);
    }

    @Override
    public void remove(ConfigBinding<?> binding) {
        writer.remove(binding);
    }

    @Override
    public void remove(ConfigSourceId sourceId, ConfigBinding<?> binding) {
        writer.remove(sourceId, binding);
    }

}
