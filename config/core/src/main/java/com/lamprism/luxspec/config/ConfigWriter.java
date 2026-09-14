package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.ConfigSourceId;

/**
 * Mutates explicitly selected configuration sources.
 *
 * @author RollW
 */
public interface ConfigWriter {
    /**
     * Writes a value to the configured default source.
     *
     * @param binding the concrete configuration binding
     * @param value   the typed value to write
     * @param <T>     the typed value
     */
    <T> void set(ConfigBinding<T> binding, T value);

    /**
     * Writes a value to an explicitly selected source.
     *
     * @param sourceId the target source ID
     * @param binding  the concrete configuration binding
     * @param value    the typed value to write
     * @param <T>      the typed value
     */
    <T> void set(ConfigSourceId sourceId, ConfigBinding<T> binding, T value);

    /**
     * Removes an entry from the configured default source.
     *
     * @param binding the concrete configuration binding
     */
    void remove(ConfigBinding<?> binding);

    /**
     * Removes an entry from an explicitly selected source.
     *
     * @param sourceId the target source ID
     * @param binding  the concrete configuration binding
     */
    void remove(ConfigSourceId sourceId, ConfigBinding<?> binding);

    /**
     * Writes a value to the configured default source.
     *
     * @param spec  the configuration definition
     * @param value the typed value to write
     * @param <T>   the typed value
     */
    default <T> void set(ConfigSpec<T> spec, T value) {
        set(spec.bind(), value);
    }

    /**
     * Writes a value to an explicitly selected source.
     *
     * @param sourceId the target source ID
     * @param spec     the configuration definition
     * @param value    the typed value to write
     * @param <T>      the typed value
     */
    default <T> void set(ConfigSourceId sourceId, ConfigSpec<T> spec, T value) {
        set(sourceId, spec.bind(), value);
    }

    /**
     * Removes a definition from the configured default source.
     *
     * @param spec the configuration definition
     */
    default void remove(ConfigSpec<?> spec) {
        remove(spec.bind());
    }

    /**
     * Removes a definition from an explicitly selected source.
     *
     * @param sourceId the target source ID
     * @param spec     the configuration definition
     */
    default void remove(ConfigSourceId sourceId, ConfigSpec<?> spec) {
        remove(sourceId, spec.bind());
    }

}
