package com.lamprism.luxspec.config.source.process;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Reads configuration values from a snapshot of process environment variables.
 *
 * <p>By default, dotted, dashed, and underscored key segments map to uppercase
 * underscore-separated environment names. A mapping function may instead own the complete
 * external name. The source is bootstrap-safe and does not define source assembly order for other
 * sources.</p>
 *
 * @author RollW
 */
public class EnvironmentConfigSource implements ConfigSource {
    private static final ConfigSourceId DEFAULT_ID = ConfigSourceId.of("environment");
    private final ConfigSourceId id;
    private final Map<String, String> values;
    private final Function<? super ConfigKey, String> nameMapper;

    /**
     * Creates a source from the current process environment.
     */
    public EnvironmentConfigSource() {
        this(DEFAULT_ID, System.getenv(), EnvironmentConfigSource::toEnvironmentName);
    }

    /**
     * Creates a source from an environment snapshot using the default ID and name mapping.
     *
     * @param values environment variable values
     * @return the environment source
     */
    public static EnvironmentConfigSource from(Map<String, String> values) {
        return new EnvironmentConfigSource(
                DEFAULT_ID,
                values,
                EnvironmentConfigSource::toEnvironmentName
        );
    }

    /**
     * Creates a source with a complete configuration-key to environment-name mapping.
     *
     * <p>The mapper is called for every lookup and must return the complete, non-blank external
     * name.</p>
     *
     * @param id         the source instance identifier
     * @param values     environment variable values
     * @param nameMapper the complete external-name mapping
     */
    public EnvironmentConfigSource(
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
        String value = values.get(ProcessConfigNames.map(nameMapper, key));
        return value == null ? ConfigEntry.absent() : ConfigEntry.present(value);
    }

    private static String toEnvironmentName(ConfigKey key) {
        String value = key.getValue();
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '.' || character == '-' || character == '_') {
                result.append('_');
            } else {
                result.append(Character.toUpperCase(character));
            }
        }
        return result.toString();
    }
}
