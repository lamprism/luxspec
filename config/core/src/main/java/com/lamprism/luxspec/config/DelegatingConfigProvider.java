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
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.Objects;

/**
 *
 * @author RollW
 */
final class DelegatingConfigProvider implements ConfigProvider {
    private final ConfigReader reader;
    private final ConfigWriter writer;

    DelegatingConfigProvider(ConfigReader reader, ConfigWriter writer) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public ConfigSourceScope getSourceScope() {
        return reader.getSourceScope();
    }

    @Override
    public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
        return reader.get(Objects.requireNonNull(binding, "binding"));
    }

    @Override
    public <T> void set(ConfigBinding<T> binding, T value) {
        writer.set(binding, value);
    }

    @Override
    public <T> void set(ConfigSourceId sourceId, ConfigBinding<T> binding, T value) {
        writer.set(sourceId, binding, value);
    }

    @Override
    public void remove(ConfigBinding<?> binding) {
        writer.remove(binding);
    }

    @Override
    public void remove(ConfigSourceId sourceId, ConfigBinding<?> binding) {
        writer.remove(sourceId, binding);
    }

}
