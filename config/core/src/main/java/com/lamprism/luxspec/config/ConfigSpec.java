package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Defines one typed configuration value independent of its storage provider.
 *
 * @param <T> the typed value
 * @author RollW
 */
public interface ConfigSpec<T> {
    /**
     * Returns the complete key for this fixed or bound definition.
     *
     * @return the complete configuration key
     */
    ConfigKey getKey();

    /**
     * Returns the codec that converts this definition's raw value.
     *
     * @return the typed codec
     */
    ConfigCodec<T> getCodec();

    /**
     * Returns the optional value used only when every usable source is absent.
     *
     * @return the optional default value
     */
    Optional<T> getDefaultValue();

    /**
     * Reports whether administrative views and events must not reveal its value.
     *
     * @return {@code true} when the value is sensitive
     */
    boolean isSensitive();

    /**
     * Returns the capabilities required from a source that may serve this definition.
     *
     * @return immutable required source capabilities
     */
    Set<ConfigSourceCapability> getRequiredSourceCapabilities();

    /**
     * Returns attribute values required from a usable source.
     *
     * @return immutable required source attributes
     */
    Map<String, String> getRequiredSourceAttributes();

    /**
     * Returns the only source ID that may serve this definition when one is configured.
     *
     * @return the optional exact source ID
     */
    Optional<ConfigSourceId> getRequiredSourceId();
}
