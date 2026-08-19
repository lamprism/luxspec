package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.process.CommandLineConfigSource;
import com.lamprism.luxspec.config.source.process.EnvironmentConfigSource;
import com.lamprism.luxspec.config.source.process.SystemPropertyConfigSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProcessConfigSourceTest {
    @Test
    void mapsConfigurationKeysToEnvironmentNames() {
        EnvironmentConfigSource source = new EnvironmentConfigSource(
                ConfigSourceId.of("environment-test"),
                Map.of("DATABASE_SSL_MODE", "verify-ca")
        );

        assertStringValue(source.get(ConfigKey.of("database.ssl-mode")), "verify-ca");
        assertEquals(ConfigSourceScope.BOOTSTRAP, source.getScope());
    }

    @Test
    void readsExactSystemPropertyNames() {
        SystemPropertyConfigSource source = new SystemPropertyConfigSource(
                ConfigSourceId.of("properties-test"),
                Map.of("database.type", "postgresql")
        );

        assertStringValue(source.get(ConfigKey.of("database.type")), "postgresql");
    }

    @Test
    void parsesCommandLineOptionsAndRepeatedValues() {
        CommandLineConfigSource source = new CommandLineConfigSource(
                ConfigSourceId.of("command-line-test"),
                "--database.type=h2",
                "--database.options",
                "MODE=PostgreSQL",
                "--database.options=TRACE_LEVEL_SYSTEM_OUT=0",
                "--database.enabled"
        );

        assertStringValue(source.get(ConfigKey.of("database.type")), "h2");
        RawConfigValue options = source.get(ConfigKey.of("database.options")).requireRawValue();
        assertEquals(
                List.of(
                        RawConfigValue.string("MODE=PostgreSQL"),
                        RawConfigValue.string("TRACE_LEVEL_SYSTEM_OUT=0")
                ),
                options.requireList()
        );
        assertStringValue(source.get(ConfigKey.of("database.enabled")), "true");
    }

    @Test
    void ignoresArgumentsAfterTheOptionTerminator() {
        CommandLineConfigSource source = new CommandLineConfigSource(
                ConfigSourceId.of("command-line-test"),
                "--database.type=h2",
                "--",
                "--database.type=sqlite"
        );

        assertStringValue(source.get(ConfigKey.of("database.type")), "h2");
        assertSame(
                ConfigEntry.absent(),
                source.get(ConfigKey.of("database.target"))
        );
    }

    @Test
    void ignoresArgumentsWithInvalidConfigurationKeyNames() {
        CommandLineConfigSource source = new CommandLineConfigSource(
                ConfigSourceId.of("command-line-test"),
                "--database..type=h2",
                "--database/type=sqlite"
        );

        assertSame(
                ConfigEntry.absent(),
                source.get(ConfigKey.of("database.type"))
        );
    }

    private static void assertStringValue(ConfigEntry entry, String expected) {
        assertEquals(ConfigEntry.State.PRESENT, entry.getState());
        assertEquals(expected, entry.requireRawValue().requireString());
    }
}
