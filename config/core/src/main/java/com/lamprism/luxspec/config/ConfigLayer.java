package com.lamprism.luxspec.config;

import java.util.Objects;

/**
 * Places one source in the explicit order of a configuration read view.
 *
 * @author RollW
 */
public final class ConfigLayer {
    private final ConfigSource source;

    public ConfigLayer(ConfigSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public ConfigSource getSource() {
        return source;
    }
}
