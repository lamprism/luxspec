/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
