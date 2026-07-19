package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves typed configuration values through configured layers.
 *
 * @author RollW
 */
public interface ConfigReader {
    /**
     * Resolves one fixed or bound configuration definition through configured layers.
     *
     * @param spec the complete configuration definition
     * @param <T> the decoded value type
     * @return the resolved value and origin
     */
    <T> ResolvedConfig<T> get(ConfigSpec<T> spec);

    /**
     * Resolves a definition while selecting whether a configured cache may be used.
     *
     * @param spec the complete configuration definition
     * @param option the read option
     * @param <T> the decoded value type
     * @return the resolved value and origin
     */
    default <T> ResolvedConfig<T> get(ConfigSpec<T> spec, ConfigReadOption option) {
        Objects.requireNonNull(option, "option");
        return get(spec);
    }

    /**
     * Binds and resolves a parameterized definition.
     *
     * @param spec the parameterized definition
     * @param arguments the complete validated template arguments
     * @param <T> the decoded value type
     * @return the resolved value and origin
     */
    default <T> ResolvedConfig<T> get(TemplateConfigSpec<T> spec, Map<String, String> arguments) {
        return get(Objects.requireNonNull(spec, "spec").bind(arguments));
    }
}
