package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.runtime.ScopedLayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.InMemoryConfigSource;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigReaderAssemblyTest {
    private static final ConfigSpec<String> VALUE = ConfigSpec.of(
            "application.value",
            ConfigCodecs.string(),
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

    private static InMemoryConfigSource source(String id, String value) {
        return InMemoryConfigSource.fromStrings(
                ConfigSourceId.of(id),
                ConfigSourceScope.BOOTSTRAP,
                Map.of("application.value", value)
        );
    }
}
