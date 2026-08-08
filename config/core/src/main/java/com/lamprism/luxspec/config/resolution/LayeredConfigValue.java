package com.lamprism.luxspec.config.resolution;

import com.lamprism.luxspec.config.ConfigValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A typed configuration result that retains the ordered observations used to resolve it.
 *
 * <p>Layers are ordered from highest to lowest precedence. A present Source value is selected over
 * lower values, while a tombstone is selected as an explicit suppression result. Spec defaults are
 * fallback candidates and therefore do not prevent a later Source observation from taking effect.
 * Nested layered values are flattened at construction time.</p>
 *
 * @param <T> the typed value
 * @author RollW
 */
public final class LayeredConfigValue<T> implements ConfigValue<T> {
    private final List<ConfigValue<T>> layers;
    private final ConfigValue<T> effective;

    private LayeredConfigValue(List<ConfigValue<T>> layers) {
        this.layers = List.copyOf(layers);
        this.effective = selectEffective(this.layers);
    }

    /**
     * Creates a layered result and flattens nested layered results.
     *
     * @param layers the ordered layer observations
     * @param <T>    the typed value
     * @return the immutable layered result
     */
    public static <T> LayeredConfigValue<T> of(List<? extends ConfigValue<T>> layers) {
        List<ConfigValue<T>> flattened = new ArrayList<>();
        for (ConfigValue<T> layer : Objects.requireNonNull(layers, "layers")) {
            append(flattened, Objects.requireNonNull(layer, "layer"));
        }
        return new LayeredConfigValue<>(flattened);
    }

    /**
     * Returns immutable layers in precedence order.
     *
     * @return the layer observations
     */
    public List<ConfigValue<T>> getLayers() {
        return layers;
    }

    @Override
    public State getState() {
        return effective.getState();
    }

    @Override
    public @Nullable T getValue() {
        return effective.getValue();
    }

    @Override
    public ConfigValueOrigin getOrigin() {
        return effective.getOrigin();
    }

    @Override
    public String toString() {
        return "LayeredConfigValue[layerCount=" + layers.size()
                + ", state=" + getState()
                + ", origin=" + getOrigin() + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigValue<?> value)) {
            return false;
        }
        return getState() == value.getState()
                && Objects.equals(getValue(), value.getValue())
                && getOrigin().equals(value.getOrigin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getState(), getValue(), getOrigin());
    }

    private static <T> void append(List<ConfigValue<T>> target, ConfigValue<T> value) {
        if (value instanceof LayeredConfigValue<?> layered) {
            appendNested(target, layered);
            return;
        }
        target.add(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> void appendNested(List<ConfigValue<T>> target, LayeredConfigValue<?> layered) {
        for (ConfigValue<?> nested : layered.getLayers()) {
            append(target, (ConfigValue<T>) nested);
        }
    }

    private static <T> ConfigValue<T> selectEffective(List<ConfigValue<T>> layers) {
        ConfigValue<T> defaultValue = null;
        for (ConfigValue<T> layer : layers) {
            if (layer.getOrigin() instanceof ConfigValueOrigin.TombstoneOrigin) {
                return layer;
            }
            if (layer.getState() == State.ABSENT) {
                continue;
            }
            if (layer.getOrigin() instanceof ConfigValueOrigin.DefaultOrigin) {
                if (defaultValue == null) {
                    defaultValue = layer;
                }
                continue;
            }
            return layer;
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        return ConfigValue.absent();
    }
}
