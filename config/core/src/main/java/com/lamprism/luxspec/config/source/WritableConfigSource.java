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

/**
 * Adds ordinary mutation operations to the read-only source role.
 *
 * @author RollW
 */
public interface WritableConfigSource extends ConfigSource {
    /**
     * Stores one raw value for a complete key.
     *
     * @param key      the complete key
     * @param rawValue the provider-neutral raw value
     */
    void set(ConfigKey key, RawConfigValue rawValue);

    /**
     * Removes one explicit entry so lower layers may become effective.
     *
     * @param key the complete key
     */
    void remove(ConfigKey key);
}
