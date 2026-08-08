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

package com.lamprism.luxspec.security.jwt;

import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * Reads trusted JWT access-token settings from typed Luxspec configuration.
 *
 * @author RollW
 */
public final class ConfigJwtAccessTokenSettingsSource {
    private final ConfigReader reader;

    /**
     * Creates a source backed by typed Luxspec configuration.
     *
     * @param reader the typed configuration reader
     */
    public ConfigJwtAccessTokenSettingsSource(ConfigReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    /**
     * Returns the current trusted JWT access-token options.
     *
     * @return the JWT access-token options
     */
    public JwtAccessTokenOptions getOptions() {
        return new JwtAccessTokenOptions(
                read(JwtAccessTokenConfigSpecs.ACCESS_TTL),
                read(JwtAccessTokenConfigSpecs.ACCESS_ISSUER),
                Set.copyOf(read(JwtAccessTokenConfigSpecs.ACCESS_AUDIENCES)),
                read(JwtAccessTokenConfigSpecs.ACCESS_CLOCK_SKEW)
        );
    }

    /**
     * Returns the provider-owned key-set name referenced by JWT configuration.
     *
     * @return the configured key-set name
     */
    public String getKeySetName() {
        return requireText(read(JwtAccessTokenConfigSpecs.KEY_SET_NAME), "keySetName");
    }

    private <T> T read(ConfigSpec<T> spec) {
        T value = readValue(spec);
        if (value == null) {
            throw new IllegalStateException(
                    "Required JWT configuration is not available: " + spec.bind().getKey().getValue()
            );
        }
        return value;
    }

    private <T> @Nullable T readValue(ConfigSpec<T> spec) {
        return reader.get(spec).getValue();
    }

    private String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
