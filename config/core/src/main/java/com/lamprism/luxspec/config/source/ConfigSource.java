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

package com.lamprism.luxspec.config.source;

import com.lamprism.luxspec.config.ConfigKey;

import java.util.Map;

/**
 * Reads raw entries for one configuration source instance.
 *
 * @author RollW
 */
public interface ConfigSource {
    /**
     * Returns the stable source instance identifier.
     *
     * @return the source identifier
     */
    ConfigSourceId getId();

    /**
     * Returns the lifecycle scope in which this source is available.
     *
     * @return the source scope
     */
    ConfigSourceScope getScope();

    /**
     * Returns provider-defined attributes used by source-selection policies.
     *
     * @return immutable source attributes
     */
    default Map<String, String> getAttributes() {
        return Map.of();
    }

    /**
     * Reads the raw entry for one complete key.
     *
     * @param key the complete configuration key
     * @return the explicit raw entry state
     */
    ConfigEntry get(ConfigKey key);

}
