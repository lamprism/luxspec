package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Objects;

/**
 * Mutates one explicitly selected configuration source.
 *
 * @author RollW
 */
public interface ConfigWriter {
    /**
     * Encodes and writes a value to the configured default source.
     *
     * @param spec the complete configuration definition
     * @param value the typed value to write
     * @param <T> the typed value
     * @throws IllegalStateException when no default target is configured
     */
    <T> void set(ConfigSpec<T> spec, T value);

    /**
     * Encodes and writes a value to the explicitly selected source.
     *
     * @param sourceId the target source ID
     * @param spec the complete configuration definition
     * @param value the typed value to write
     * @param <T> the typed value
     */
    <T> void set(ConfigSourceId sourceId, ConfigSpec<T> spec, T value);

    /**
     * Removes an entry from the configured default source so lower layers may take effect.
     *
     * @param spec the complete configuration definition
     * @throws IllegalStateException when no default target is configured
     */
    void remove(ConfigSpec<?> spec);

    /**
     * Removes an entry from an explicitly selected source so lower layers may take effect.
     *
     * @param sourceId the target source ID
     * @param spec the complete configuration definition
     */
    void remove(ConfigSourceId sourceId, ConfigSpec<?> spec);

    /**
     * Writes a mask to the configured default source that blocks lower layers.
     *
     * @param spec the complete configuration definition
     * @throws IllegalStateException when no default target is configured
     */
    void mask(ConfigSpec<?> spec);

    /**
     * Writes a mask to an explicitly selected source that blocks lower layers.
     *
     * @param sourceId the target source ID
     * @param spec the complete configuration definition
     */
    void mask(ConfigSourceId sourceId, ConfigSpec<?> spec);

    /**
     * Binds and writes a parameterized definition through the default source.
     *
     * @param spec the parameterized definition
     * @param arguments the complete template arguments
     * @param value the typed value to write
     * @param <T> the typed value
     */
    default <T> void set(TemplateConfigSpec<T> spec, Map<String, String> arguments, T value) {
        set(Objects.requireNonNull(spec, "spec").bind(arguments), value);
    }

    /**
     * Binds and writes a parameterized definition through an explicit source.
     *
     * @param sourceId the target source ID
     * @param spec the parameterized definition
     * @param arguments the complete template arguments
     * @param value the typed value to write
     * @param <T> the typed value
     */
    default <T> void set(
            ConfigSourceId sourceId,
            TemplateConfigSpec<T> spec,
            Map<String, String> arguments,
            T value
    ) {
        set(sourceId, Objects.requireNonNull(spec, "spec").bind(arguments), value);
    }
}
