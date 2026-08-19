package com.lamprism.luxspec.config.source;

import com.lamprism.luxspec.config.ConfigKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe mutable configuration source backed by process memory.
 *
 * <p>This source is useful for tests, local applications, and explicitly assembled bootstrap
 * configuration. It does not persist values or define source precedence.</p>
 *
 * @author RollW
 */
public class InMemoryConfigSource implements TombstoneConfigSource {
    private final ConfigSourceId id;
    private final ConfigSourceScope scope;
    private final Map<String, String> attributes;
    private final ConcurrentMap<ConfigKey, ConfigEntry> entries;

    /**
     * Creates an empty memory source.
     *
     * @param id    the stable source instance identifier
     * @param scope the lifecycle scope in which the source is available
     */
    public InMemoryConfigSource(ConfigSourceId id, ConfigSourceScope scope) {
        this(id, scope, Map.of(), Map.of());
    }

    /**
     * Creates a memory source with initial raw entries.
     *
     * @param id             the stable source instance identifier
     * @param scope          the lifecycle scope in which the source is available
     * @param initialEntries the entries copied into the source before it is published
     */
    public InMemoryConfigSource(
            ConfigSourceId id,
            ConfigSourceScope scope,
            Map<ConfigKey, ConfigEntry> initialEntries
    ) {
        this(id, scope, initialEntries, Map.of());
    }

    /**
     * Creates a memory source with initial raw entries and source metadata.
     *
     * @param id             the stable source instance identifier
     * @param scope          the lifecycle scope in which the source is available
     * @param initialEntries the entries copied into the source before it is published
     * @param attributes     provider-defined source metadata
     */
    public InMemoryConfigSource(
            ConfigSourceId id,
            ConfigSourceScope scope,
            Map<ConfigKey, ConfigEntry> initialEntries,
            Map<String, String> attributes
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.entries = new ConcurrentHashMap<>(copyEntries(initialEntries));
        this.attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    /**
     * Creates a memory source from flat string values.
     *
     * @param id     the stable source instance identifier
     * @param scope  the lifecycle scope in which the source is available
     * @param values the complete configuration keys and string values
     * @return the writable memory source
     */
    public static InMemoryConfigSource fromStrings(
            ConfigSourceId id,
            ConfigSourceScope scope,
            Map<String, String> values
    ) {
        return fromStrings(id, scope, values, Map.of());
    }

    /**
     * Creates a memory source from flat string values and source metadata.
     *
     * @param id         the stable source instance identifier
     * @param scope      the lifecycle scope in which the source is available
     * @param values     the complete configuration keys and string values
     * @param attributes provider-defined source metadata
     * @return the writable memory source
     */
    public static InMemoryConfigSource fromStrings(
            ConfigSourceId id,
            ConfigSourceScope scope,
            Map<String, String> values,
            Map<String, String> attributes
    ) {
        Map<ConfigKey, ConfigEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> value : Objects.requireNonNull(values, "values").entrySet()) {
            entries.put(
                    ConfigKey.of(value.getKey()),
                    ConfigEntry.present(Objects.requireNonNull(value.getValue(), "value"))
            );
        }
        return new InMemoryConfigSource(id, scope, entries, attributes);
    }

    /**
     * Creates a memory source from provider-neutral raw entries.
     *
     * @param id      the stable source instance identifier
     * @param scope   the lifecycle scope in which the source is available
     * @param entries the complete keys and raw entry states
     * @return the writable memory source
     */
    public static InMemoryConfigSource fromEntries(
            ConfigSourceId id,
            ConfigSourceScope scope,
            Map<ConfigKey, ConfigEntry> entries
    ) {
        return fromEntries(id, scope, entries, Map.of());
    }

    /**
     * Creates a memory source from provider-neutral raw entries and source metadata.
     *
     * @param id         the stable source instance identifier
     * @param scope      the lifecycle scope in which the source is available
     * @param entries    the complete keys and raw entry states
     * @param attributes provider-defined source metadata
     * @return the writable memory source
     */
    public static InMemoryConfigSource fromEntries(
            ConfigSourceId id,
            ConfigSourceScope scope,
            Map<ConfigKey, ConfigEntry> entries,
            Map<String, String> attributes
    ) {
        return new InMemoryConfigSource(id, scope, entries, attributes);
    }

    @Override
    public ConfigSourceId getId() {
        return id;
    }

    @Override
    public ConfigSourceScope getScope() {
        return scope;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public ConfigEntry get(ConfigKey key) {
        return entries.getOrDefault(Objects.requireNonNull(key, "key"), ConfigEntry.absent());
    }

    @Override
    public void set(ConfigKey key, RawConfigValue rawValue) {
        entries.put(
                Objects.requireNonNull(key, "key"),
                ConfigEntry.present(Objects.requireNonNull(rawValue, "rawValue"))
        );
    }

    @Override
    public void remove(ConfigKey key) {
        entries.remove(Objects.requireNonNull(key, "key"));
    }

    @Override
    public void writeTombstone(ConfigKey key) {
        entries.put(Objects.requireNonNull(key, "key"), ConfigEntry.tombstone());
    }

    private static Map<ConfigKey, ConfigEntry> copyEntries(
            Map<ConfigKey, ConfigEntry> initialEntries
    ) {
        Map<ConfigKey, ConfigEntry> values = new LinkedHashMap<>();
        for (Map.Entry<ConfigKey, ConfigEntry> entry : Objects.requireNonNull(
                initialEntries,
                "initialEntries"
        ).entrySet()) {
            values.put(
                    Objects.requireNonNull(entry.getKey(), "key"),
                    Objects.requireNonNull(entry.getValue(), "entry")
            );
        }
        return Map.copyOf(values);
    }
}
