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

import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.value.ImmutableConfigValue;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents one typed configuration result or source observation.
 *
 * <p>The explicit state distinguishes an absent result from a present value. Invalid raw entries
 * and resolution failures are reported by the Source and resolution contracts rather than
 * represented as typed values. An optional Source tombstone is represented as an absent result with
 * a tombstone origin.</p>
 *
 * @param <T> the typed value
 * @author RollW
 */
public interface ConfigValue<T> {
    /**
     * Identifies the state of a typed configuration result.
     */
    enum State {
        /**
         * A valid typed value is available.
         */
        PRESENT,
        /**
         * No value was supplied by this observation.
         */
        ABSENT
    }

    /**
     * Creates a present value with an explicit origin.
     *
     * @param value  the non-null typed value
     * @param origin the value origin
     * @param <T>    the typed value
     * @return the immutable configuration value
     */
    static <T> ConfigValue<T> present(T value, ConfigValueOrigin origin) {
        return ImmutableConfigValue.of(State.PRESENT, value, origin);
    }

    /**
     * Creates a value supplied by one source.
     *
     * @param value    the non-null typed value
     * @param sourceId the supplying source ID
     * @param <T>      the typed value
     * @return the source value
     */
    static <T> ConfigValue<T> source(T value, ConfigSourceId sourceId) {
        return present(value, new ConfigValueOrigin.SourceOrigin(sourceId));
    }

    /**
     * Creates a value supplied by the Spec default.
     *
     * @param value the non-null default value
     * @param <T>   the typed value
     * @return the default value
     */
    static <T> ConfigValue<T> defaultValue(T value) {
        return present(value, ConfigValueOrigin.DefaultOrigin.INSTANCE);
    }

    /**
     * Creates an absent result without a source observation.
     *
     * @param <T> the typed value
     * @return the absent value
     */
    static <T> ConfigValue<T> absent() {
        return absent(ConfigValueOrigin.AbsentOrigin.INSTANCE);
    }

    /**
     * Creates an absent observation from one source.
     *
     * @param sourceId the source that reported no entry
     * @param <T>      the typed value
     * @return the absent source observation
     */
    static <T> ConfigValue<T> absent(ConfigSourceId sourceId) {
        return absent(new ConfigValueOrigin.SourceOrigin(sourceId));
    }

    /**
     * Creates an absent result with an explicit origin.
     *
     * @param origin the non-null absent origin
     * @param <T>    the typed value
     * @return the absent value
     */
    static <T> ConfigValue<T> absent(ConfigValueOrigin origin) {
        return ImmutableConfigValue.of(State.ABSENT, null, origin);
    }

    /**
     * Returns the explicit result state.
     *
     * @return the result state
     */
    State getState();

    /**
     * Returns the typed value when the state is present.
     *
     * @return the typed value, or {@code null} for other states
     */
    @Nullable T getValue();

    /**
     * Returns the explicit result origin.
     *
     * @return the result origin
     */
    ConfigValueOrigin getOrigin();

    /**
     * Reports whether this result contains a typed value.
     *
     * @return {@code true} only for the present state
     */
    default boolean hasValue() {
        return getState() == State.PRESENT;
    }

    /**
     * Returns the typed value or fails when the result has no value.
     *
     * @return the non-null typed value
     * @throws IllegalStateException when this result is not present
     */
    default T requireValue() {
        if (!hasValue()) {
            throw new IllegalStateException("Configuration result does not contain a value");
        }
        return Objects.requireNonNull(getValue(), "value");
    }
}
