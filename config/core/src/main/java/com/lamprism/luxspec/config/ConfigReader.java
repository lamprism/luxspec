package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves typed configuration values through configured sources.
 *
 * @author RollW
 */
public interface ConfigReader {
    /**
     * Resolves one validated concrete binding.
     *
     * @param binding the concrete configuration binding
     * @param <T>     the decoded value type
     * @return the resolved configuration value
     */
    <T> ConfigValue<T> get(ConfigBinding<T> binding);

    /**
     * Resolves a fixed or parameterized definition without exposing binding mechanics to callers.
     *
     * @param spec the configuration definition
     * @param <T>  the decoded value type
     * @return the resolved configuration value
     */
    default <T> ConfigValue<T> get(ConfigSpec<T> spec) {
        return get(Objects.requireNonNull(spec, "spec").bind());
    }

    /**
     * Resolves a definition for explicit path arguments.
     *
     * @param spec      the configuration definition
     * @param arguments the path arguments
     * @param <T>       the decoded value type
     * @return the resolved configuration value
     */
    default <T> ConfigValue<T> get(ConfigSpec<T> spec, Map<String, String> arguments) {
        return get(Objects.requireNonNull(spec, "spec").bind(arguments));
    }
}
