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

import com.lamprism.luxspec.config.provider.CombinedConfigProvider;
import com.lamprism.luxspec.config.resolution.ConfigResolutionException;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.runtime.ScopedLayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.InMemoryConfigSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author RollW
 */
class ConfigReaderAssemblyTest {
    private static final ConfigSpec<String> VALUE = ConfigSpec.of(
            "application.value",
            ConfigCodec.string(),
            null,
            false
    );

    @Test
    void preservesTheAssemblyOrderAsPrecedence() {
        InMemoryConfigSource higher = source("higher", "higher-value");
        InMemoryConfigSource lower = source("lower", "lower-value");

        ConfigReader reader = ScopedLayeredConfigReader.bootstrap(List.of(higher, lower));

        assertEquals("higher-value", reader.get(VALUE).getValue());
    }

    @Test
    void exposesTheValidatedBootstrapScope() {
        ConfigReader reader = ScopedLayeredConfigReader.bootstrap(List.of(
                source("bootstrap", "value")
        ));

        assertEquals(ConfigSourceScope.BOOTSTRAP, reader.getSourceScope());
    }

    @Test
    void stopsCombinedReadsAtAnObservedTombstone() {
        InMemoryConfigSource tombstone = InMemoryConfigSource.fromEntries(
                ConfigSourceId.of("higher-tombstone"),
                ConfigSourceScope.RUNTIME,
                Map.of(ConfigKey.of("application.value"), ConfigEntry.tombstone())
        );
        AtomicInteger suppressedReads = new AtomicInteger();
        ConfigReader failingLowerReader = new ConfigReader() {
            @Override
            public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
                suppressedReads.incrementAndGet();
                throw new ConfigResolutionException();
            }
        };
        CombinedConfigProvider provider = new CombinedConfigProvider(
                List.of(new LayeredConfigReader(List.of(tombstone)), failingLowerReader),
                noOpWriter()
        );

        ConfigValue<String> value = provider.get(VALUE);

        assertEquals(false, value.hasValue());
        assertEquals(1, ((com.lamprism.luxspec.config.resolution.LayeredConfigValue<?>) value)
                .getLayers().size());
        assertEquals(0, suppressedReads.get());
    }

    private static InMemoryConfigSource source(String id, String value) {
        return InMemoryConfigSource.fromStrings(
                ConfigSourceId.of(id),
                ConfigSourceScope.BOOTSTRAP,
                Map.of("application.value", value)
        );
    }

    private static ConfigWriter noOpWriter() {
        return new ConfigWriter() {
            @Override
            public <T> void set(ConfigBinding<T> binding, T value) {
            }

            @Override
            public <T> void set(ConfigSourceId sourceId, ConfigBinding<T> binding, T value) {
            }

            @Override
            public void remove(ConfigBinding<?> binding) {
            }

            @Override
            public void remove(ConfigSourceId sourceId, ConfigBinding<?> binding) {
            }
        };
    }
}
