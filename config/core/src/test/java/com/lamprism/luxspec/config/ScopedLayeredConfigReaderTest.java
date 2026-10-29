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

import com.lamprism.luxspec.config.runtime.ScopedLayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.InMemoryConfigSource;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class ScopedLayeredConfigReaderTest {
    @Test
    void rejectsSourcesFromAnotherScope() {
        InMemoryConfigSource bootstrap = InMemoryConfigSource.fromStrings(
                ConfigSourceId.of("bootstrap"),
                ConfigSourceScope.BOOTSTRAP,
                Map.of()
        );
        InMemoryConfigSource runtime = InMemoryConfigSource.fromStrings(
                ConfigSourceId.of("runtime"),
                ConfigSourceScope.RUNTIME,
                Map.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ScopedLayeredConfigReader(
                        ConfigSourceScope.BOOTSTRAP,
                        List.of(bootstrap, runtime)
                )
        );
    }

    @Test
    void exposesTheScopeUsedToAssembleTheReader() {
        ConfigReader reader = new ScopedLayeredConfigReader(
                ConfigSourceScope.BOOTSTRAP,
                List.of(InMemoryConfigSource.fromStrings(
                        ConfigSourceId.of("bootstrap"),
                        ConfigSourceScope.BOOTSTRAP,
                        Map.of()
                ))
        );

        assertEquals(ConfigSourceScope.BOOTSTRAP, reader.getSourceScope());
    }
}
