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

import com.lamprism.luxspec.config.provider.DelegatingConfigProvider;

import java.util.Objects;

/**
 * Assembled configuration runtime role combining typed reads and writes.
 *
 * <p>Source adapters remain below this role. Providers compose and decorate readers and writers,
 * including layered resolution, caching, event publication, and read-only behavior.</p>
 *
 * @author RollW
 */
public interface ConfigProvider extends ConfigReader, ConfigWriter {
    /**
     * Combines independent read and write roles into one Provider.
     *
     * @param reader the typed reader
     * @param writer the typed writer
     * @return the delegating Provider
     */
    static ConfigProvider of(ConfigReader reader, ConfigWriter writer) {
        return new DelegatingConfigProvider(
                Objects.requireNonNull(reader, "reader"),
                Objects.requireNonNull(writer, "writer")
        );
    }
}
