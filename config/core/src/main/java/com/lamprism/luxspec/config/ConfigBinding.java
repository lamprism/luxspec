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

import java.util.Map;
import java.util.Objects;

/**
 * Represents one validated concrete key produced by a configuration definition.
 *
 * @param <T> the typed value
 * @author RollW
 */
public final class ConfigBinding<T> {
    private final ConfigSpec<T> spec;
    private final ConfigKey key;
    private final Map<String, String> arguments;

    ConfigBinding(ConfigSpec<T> spec, Map<String, String> arguments) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        this.key = spec.getKey().bind(this.arguments);
    }

    /**
     * Returns the definition that owns this binding.
     *
     * @return the configuration definition
     */
    public ConfigSpec<T> getSpec() {
        return spec;
    }

    /**
     * Returns the complete configuration key.
     *
     * @return the complete key
     */
    public ConfigKey getKey() {
        return key;
    }

    /**
     * Returns the immutable arguments used to render the key.
     *
     * @return the binding arguments
     */
    public Map<String, String> getArguments() {
        return arguments;
    }
}
