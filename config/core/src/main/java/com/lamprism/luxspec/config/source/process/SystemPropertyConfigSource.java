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
import java.util.function.Function;

/**
 * Reads configuration values from a snapshot of JVM system properties.
 *
 * <p>By default, a complete configuration key maps to the identical property name. A mapping
 * function may instead own the complete external name.</p>
 *
 * @author RollW
 */
public class SystemPropertyConfigSource implements ConfigSource {
    private static final ConfigSourceId DEFAULT_ID = ConfigSourceId.of("system-properties");
    private final ConfigSourceId id;
    private final Map<String, String> values;
    private final Function<? super ConfigKey, String> nameMapper;

    /**
     * Creates a source from the current JVM system properties.
     */
    public SystemPropertyConfigSource() {
        this(DEFAULT_ID, snapshot(), ConfigKey::getValue);
    }

    /**
     * Creates a source from a property snapshot using the default ID and identity mapping.
     *
     * @param values system property values
     * @return the system-property source
     */
    public static SystemPropertyConfigSource from(Map<String, String> values) {
        return new SystemPropertyConfigSource(DEFAULT_ID, values, ConfigKey::getValue);
    }

    /**
     * Creates a source with a complete configuration-key to property-name mapping.
     *
     * <p>The mapper is called for every lookup and must return the complete, non-blank external
     * name.</p>
     *
     * @param id         the source instance identifier
     * @param values     system property values
     * @param nameMapper the complete external-name mapping
     */
    public SystemPropertyConfigSource(
            ConfigSourceId id,
            Map<String, String> values,
            Function<? super ConfigKey, String> nameMapper
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.values = Map.copyOf(Objects.requireNonNull(values, "values"));
        this.nameMapper = Objects.requireNonNull(nameMapper, "nameMapper");
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
        String value = values.get(ProcessConfigName.map(nameMapper, key));
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
