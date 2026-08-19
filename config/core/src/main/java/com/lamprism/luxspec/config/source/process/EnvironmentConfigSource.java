package com.lamprism.luxspec.config.source.process;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.Map;
import java.util.Objects;

/**
 * Reads configuration values from a snapshot of process environment variables.
 *
 * <p>Dotted, dashed, and underscored key segments map to uppercase underscore-separated
 * environment names. The source is bootstrap-safe and does not define source assembly order for
 * other sources.</p>
 *
 * @author RollW
 */
public class EnvironmentConfigSource implements ConfigSource {
    private static final ConfigSourceId DEFAULT_ID = ConfigSourceId.of("environment");
    private final ConfigSourceId id;
    private final Map<String, String> values;

    /**
     * Creates a source from the current process environment.
     */
    public EnvironmentConfigSource() {
        this(DEFAULT_ID, System.getenv());
    }

    /**
     * Creates a source from the supplied environment snapshot.
     *
     * @param id     the source instance identifier
     * @param values environment variable values
     */
    public EnvironmentConfigSource(
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
        String value = values.get(toEnvironmentName(Objects.requireNonNull(key, "key").getValue()));
        return value == null ? ConfigEntry.absent() : ConfigEntry.present(value);
    }

    private static String toEnvironmentName(String key) {
        StringBuilder result = new StringBuilder(key.length());
        for (int index = 0; index < key.length(); index++) {
            char character = key.charAt(index);
            if (character == '.' || character == '-' || character == '_') {
                result.append('_');
            } else {
                result.append(Character.toUpperCase(character));
            }
        }
        return result.toString();
    }
}
