package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.toml.TomlConfigSource;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TomlConfigSourceTest {
    @Test
    void preservesNativeTomlScalarKinds() {
        TomlConfigSource source = new TomlConfigSource(
                ConfigSourceId.of("toml"),
                ConfigSourceScope.BOOTSTRAP,
                new ByteArrayInputStream(
                        "numeric-size = 1000\ntext-size = \"1KB\"\n".getBytes(StandardCharsets.UTF_8)
                )
        );

        ConfigEntry numeric = source.get(ConfigKey.of("numeric-size"));
        ConfigEntry text = source.get(ConfigKey.of("text-size"));

        assertEquals(RawConfigValue.ScalarKind.INTEGER, numeric.requireRawValue().getScalarKind());
        assertEquals(1000, numeric.requireRawValue().requireInteger());
        assertEquals(RawConfigValue.ScalarKind.STRING, text.requireRawValue().getScalarKind());
        assertEquals("1KB", text.requireRawValue().requireString());
    }

    @Test
    void preservesNativeBooleanDecimalListAndNestedValues() {
        TomlConfigSource source = source("""
                enabled = true
                ratio = 1.25
                sizes = [1000, "1KB", true]
                
                [database]
                port = 5432
                """);

        assertEquals(
                RawConfigValue.ScalarKind.BOOLEAN,
                source.get(ConfigKey.of("enabled")).requireRawValue().getScalarKind()
        );
        assertEquals(
                new BigDecimal("1.25"),
                source.get(ConfigKey.of("ratio")).requireRawValue().requireDecimal()
        );
        assertEquals(
                List.of(
                        RawConfigValue.integer(1000),
                        RawConfigValue.string("1KB"),
                        RawConfigValue.booleanValue(true)
                ),
                source.get(ConfigKey.of("sizes")).requireRawValue().requireList()
        );
        assertEquals(
                5432,
                source.get(ConfigKey.of("database.port")).requireRawValue().requireInteger()
        );
    }

    @Test
    void reportsUnsupportedTomlStructuresAsInvalidEntries() {
        TomlConfigSource source = source("""
                [database]
                settings = {timeout = 10}
                values = [[1]]
                """);

        ConfigEntry object = source.get(ConfigKey.of("database.settings"));
        ConfigEntry nestedList = source.get(ConfigKey.of("database.values"));

        assertEquals(ConfigEntry.State.INVALID, object.getState());
        assertEquals("TOML value is an unsupported object", object.requireInvalidDescription());
        assertEquals(ConfigEntry.State.INVALID, nestedList.getState());
        assertEquals(
                "TOML list contains an unsupported element",
                nestedList.requireInvalidDescription()
        );
    }

    @Test
    void rejectsMalformedTomlDocuments() {
        assertThrows(IllegalArgumentException.class, () -> source("invalid = ["));
    }

    private static TomlConfigSource source(String content) {
        return new TomlConfigSource(
                ConfigSourceId.of("toml"),
                ConfigSourceScope.BOOTSTRAP,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
    }
}
