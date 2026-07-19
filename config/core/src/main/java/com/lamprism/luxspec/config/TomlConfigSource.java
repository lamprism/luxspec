package com.lamprism.luxspec.config;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reads scalar and list configuration values from one immutable TOML document.
 *
 * @author RollW
 */
public final class TomlConfigSource implements ConfigSource {
    private static final Set<ConfigSourceCapability> CAPABILITIES = Set.of(ConfigSourceCapability.READ);
    private final ConfigSourceId id;
    private final Map<String, Object> document;

    /**
     * Reads one TOML document and assigns it a stable source ID.
     *
     * @param id the configured source instance ID
     * @param input the TOML input to consume
     */
    public TomlConfigSource(ConfigSourceId id, InputStream input) {
        this.id = Objects.requireNonNull(id, "id");
        this.document = readDocument(input);
    }

    @Override
    public ConfigSourceId getId() {
        return id;
    }

    @Override
    public Set<ConfigSourceCapability> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public ConfigEntry get(ConfigKey key) {
        Object value = find(Objects.requireNonNull(key, "key"));
        if (value == null) {
            return ConfigEntry.absent();
        }
        if (value instanceof List<?> values) {
            return listEntry(values);
        }
        if (value instanceof Map<?, ?>) {
            return ConfigEntry.invalid("TOML value is an unsupported object");
        }
        return ConfigEntry.present(String.valueOf(value));
    }

    private static Map<String, Object> readDocument(InputStream input) {
        try (InputStream nonNullInput = Objects.requireNonNull(input, "input")) {
            Map<?, ?> parsed = new TomlMapper().readValue(nonNullInput, Map.class);
            Map<String, Object> document = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : parsed.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("TOML document contains a non-string key");
                }
                document.put(key, entry.getValue());
            }
            return Map.copyOf(document);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read TOML configuration", exception);
        }
    }

    private Object find(ConfigKey key) {
        Object current = document;
        String value = key.getValue();
        int segmentStart = 0;
        for (int index = 0; index <= value.length(); index++) {
            if (index != value.length() && value.charAt(index) != '.') {
                continue;
            }
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(value.substring(segmentStart, index));
            if (current == null) {
                return null;
            }
            segmentStart = index + 1;
        }
        return current;
    }

    private ConfigEntry listEntry(List<?> values) {
        ArrayList<String> elements = new ArrayList<>();
        for (Object value : values) {
            if (value == null || value instanceof Map<?, ?> || value instanceof List<?>) {
                return ConfigEntry.invalid("TOML list contains an unsupported element");
            }
            elements.add(String.valueOf(value));
        }
        return ConfigEntry.present(elements);
    }
}
