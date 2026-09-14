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
