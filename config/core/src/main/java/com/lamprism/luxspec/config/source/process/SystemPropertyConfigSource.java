package com.lamprism.luxspec.config.source.process;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Reads configuration values from a snapshot of JVM system properties.
 *
 * @author RollW
 */
public class SystemPropertyConfigSource implements ConfigSource {
    private static final ConfigSourceId DEFAULT_ID = ConfigSourceId.of("system-properties");
    private final ConfigSourceId id;
    private final Map<String, String> values;

    /**
     * Creates a source from the current JVM system properties.
     */
    public SystemPropertyConfigSource() {
        this(DEFAULT_ID, snapshot());
    }

    /**
     * Creates a source from the supplied system-property snapshot.
     *
     * @param id     the source instance identifier
     * @param values system property values
     */
    public SystemPropertyConfigSource(
            ConfigSourceId id,
            Map<String, String> values
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.values = Map.copyOf(Objects.requireNonNull(values, "values"));
    }

    @Override
    public ConfigSourceId getId() {
        return id;
    }

    @Override
    public ConfigSourceScope getScope() {
        return ConfigSourceScope.BOOTSTRAP;
    }

    @Override
    public ConfigEntry get(ConfigKey key) {
        String value = values.get(Objects.requireNonNull(key, "key").getValue());
        return value == null ? ConfigEntry.absent() : ConfigEntry.present(value);
    }

    private static Map<String, String> snapshot() {
        Properties properties = System.getProperties();
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (value != null) {
                values.put(name, value);
            }
        }
        return values;
    }
}
