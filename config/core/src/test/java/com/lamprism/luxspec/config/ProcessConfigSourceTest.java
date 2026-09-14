package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.process.CommandLineConfigAssignment;
import com.lamprism.luxspec.config.source.process.CommandLineConfigFormat;
import com.lamprism.luxspec.config.source.process.CommandLineConfigSource;
import com.lamprism.luxspec.config.source.process.EnvironmentConfigSource;
import com.lamprism.luxspec.config.source.process.SystemPropertyConfigSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessConfigSourceTest {
    @Test
    void mapsConfigurationKeysToEnvironmentNames() {
        EnvironmentConfigSource source = EnvironmentConfigSource.from(
                Map.of("DATABASE_SSL_MODE", "verify-ca")
        );

        assertStringValue(source.get(ConfigKey.of("database.ssl-mode")), "verify-ca");
        assertEquals(ConfigSourceScope.BOOTSTRAP, source.getScope());
    }

    @Test
    void readsExactSystemPropertyNames() {
        SystemPropertyConfigSource source = SystemPropertyConfigSource.from(
                Map.of("database.type", "postgresql")
        );

        assertStringValue(source.get(ConfigKey.of("database.type")), "postgresql");
    }

    @Test
    void mapsCompleteExternalNames() {
        ConfigKey key = ConfigKey.of("database.ssl-mode");
        EnvironmentConfigSource affixedEnvironment = new EnvironmentConfigSource(
                ConfigSourceId.of("environment-affixed"),
                Map.of("LUX_DATABASE_SSL_MODE_VALUE", "verify-full"),
                ignored -> "LUX_DATABASE_SSL_MODE_VALUE"
        );
        EnvironmentConfigSource mappedEnvironment = new EnvironmentConfigSource(
                ConfigSourceId.of("environment-mapped"),
                Map.of("DATABASE_SSL", "verify-ca"),
                ignored -> "DATABASE_SSL"
        );
        SystemPropertyConfigSource affixedProperties = new SystemPropertyConfigSource(
                ConfigSourceId.of("properties-affixed"),
                Map.of("lux.database.ssl-mode.value", "required"),
                ignored -> "lux.database.ssl-mode.value"
        );

        assertStringValue(affixedEnvironment.get(key), "verify-full");
        assertStringValue(mappedEnvironment.get(key), "verify-ca");
        assertStringValue(affixedProperties.get(key), "required");
    }

    @Test
    void parsesCommandLineOptionsAndRepeatedValues() {
        CommandLineConfigSource source = CommandLineConfigSource.from(
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
    void restrictsCommandLineValueSyntax() {
        List<String> arguments = List.of(
                "--database.inline=h2",
                "--database.separate",
                "postgresql"
        );
        CommandLineConfigSource inline = CommandLineConfigSource.builder(arguments)
                .id(ConfigSourceId.of("command-line-inline"))
                .format(CommandLineConfigFormat.inline())
                .build();
        CommandLineConfigSource separate = CommandLineConfigSource.builder(arguments)
                .id(ConfigSourceId.of("command-line-separate"))
                .format(CommandLineConfigFormat.separate())
                .build();

        assertStringValue(inline.get(ConfigKey.of("database.inline")), "h2");
        assertSame(ConfigEntry.absent(), inline.get(ConfigKey.of("database.separate")));
        assertSame(ConfigEntry.absent(), separate.get(ConfigKey.of("database.inline")));
        assertStringValue(separate.get(ConfigKey.of("database.separate")), "postgresql");
    }

    @Test
    void composesCommandLineFormatsLikePolicies() {
        CommandLineConfigFormat format = CommandLineConfigFormat.inline()
                .and(CommandLineConfigFormat.separate());
        CommandLineConfigSource source = CommandLineConfigSource.builder(
                        "--database.options=first",
                        "--database.options",
                        "second"
                )
                .id(ConfigSourceId.of("command-line-composed"))
                .format(format)
                .build();

        assertEquals(
                List.of(RawConfigValue.string("first"), RawConfigValue.string("second")),
                source.get(ConfigKey.of("database.options")).requireRawValue().requireList()
        );
    }

    @Test
    void acceptsAnApplicationDefinedCommandLineFormat() {
        CommandLineConfigFormat format = arguments -> List.of(
                new CommandLineConfigAssignment(0, "database.type", arguments.get(0), true)
        );
        CommandLineConfigSource source = CommandLineConfigSource.builder("sqlite")
                .id(ConfigSourceId.of("command-line-custom"))
                .format(format)
                .build();

        assertStringValue(source.get(ConfigKey.of("database.type")), "sqlite");
    }

    @Test
    void mapsCommandLineOptionNames() {
        CommandLineConfigSource affixed = CommandLineConfigSource.builder(
                        "--lux-database.type-value=h2"
                )
                .id(ConfigSourceId.of("command-line-affixed"))
                .nameAffixes("lux-", "-value")
                .build();
        CommandLineConfigSource mapped = CommandLineConfigSource.builder(
                        "--database-type=postgresql"
                )
                .id(ConfigSourceId.of("command-line-mapped"))
                .format(CommandLineConfigFormat.inline())
                .nameMapper(key -> key.getValue().replace('.', '-'))
                .build();

        assertStringValue(affixed.get(ConfigKey.of("database.type")), "h2");
        assertStringValue(mapped.get(ConfigKey.of("database.type")), "postgresql");
    }

    @Test
    void readsRepeatedAssignmentsFromAContainerOption() {
        CommandLineConfigFormat outerFormat = CommandLineConfigFormat.inlineOrSeparate();
        CommandLineConfigFormat format = outerFormat.named("config").keyValueAssignments();
        CommandLineConfigSource source = CommandLineConfigSource.builder(
                        "--config=database.type=h2",
                        "--config",
                        "database.options=MODE=PostgreSQL",
                        "--config=database.options=TRACE_LEVEL_SYSTEM_OUT=0"
                )
                .id(ConfigSourceId.of("command-line-container"))
                .format(format)
                .build();

        assertStringValue(source.get(ConfigKey.of("database.type")), "h2");
        assertEquals(
                List.of(
                        RawConfigValue.string("MODE=PostgreSQL"),
                        RawConfigValue.string("TRACE_LEVEL_SYSTEM_OUT=0")
                ),
                source.get(ConfigKey.of("database.options")).requireRawValue().requireList()
        );
    }

    @Test
    void appliesTheSuppliedOuterFormatToContainerOptions() {
        CommandLineConfigSource source = CommandLineConfigSource.builder(
                        "--config=database.type=h2",
                        "--config",
                        "database.target=ignored"
                )
                .id(ConfigSourceId.of("command-line-inline-container"))
                .format(CommandLineConfigFormat.inline()
                        .named("config")
                        .keyValueAssignments())
                .build();

        assertStringValue(source.get(ConfigKey.of("database.type")), "h2");
        assertSame(ConfigEntry.absent(), source.get(ConfigKey.of("database.target")));
    }

    @Test
    void combinesDirectAndContainerValuesInArgumentOrder() {
        CommandLineConfigFormat outerFormat = CommandLineConfigFormat.inlineOrSeparate();
        CommandLineConfigFormat directFormat = outerFormat.excluding("lux-config");
        CommandLineConfigFormat containerFormat = outerFormat.named("lux-config")
                .keyValueAssignments();
        CommandLineConfigSource source = CommandLineConfigSource.builder(
                        "--lux-config=database.options=first",
                        "--database.options=second"
                )
                .id(ConfigSourceId.of("command-line-combined"))
                .format(directFormat.and(containerFormat))
                .build();

        assertEquals(
                List.of(RawConfigValue.string("first"), RawConfigValue.string("second")),
                source.get(ConfigKey.of("database.options")).requireRawValue().requireList()
        );
        assertSame(ConfigEntry.absent(), source.get(ConfigKey.of("lux-config")));
    }

    @Test
    void rejectsAContainerFlagWithoutAnAssignment() {
        CommandLineConfigFormat format = CommandLineConfigFormat.inlineOrSeparate()
                .named("config")
                .keyValueAssignments();

        assertThrows(
                IllegalArgumentException.class,
                () -> CommandLineConfigSource.builder("--config")
                        .id(ConfigSourceId.of("command-line-container"))
                        .format(format)
                        .build()
        );
    }

    @Test
    void ignoresArgumentsAfterTheOptionTerminator() {
        CommandLineConfigSource source = CommandLineConfigSource.from(
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
        CommandLineConfigSource source = CommandLineConfigSource.from(
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
