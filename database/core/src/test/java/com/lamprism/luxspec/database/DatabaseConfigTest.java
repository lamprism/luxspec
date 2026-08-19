package com.lamprism.luxspec.database;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.runtime.ScopedLayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseConfigTest {
    @Test
    void rejectsNetworkDatabaseWithoutAName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseConfig.builder(
                        DatabaseType.POSTGRESQL,
                        DatabaseTarget.network("localhost")
                ).build()
        );
    }

    @Test
    void rejectsDatabaseNamesThatCanChangeJdbcStructure() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseConfig.builder(
                        DatabaseType.POSTGRESQL,
                        DatabaseTarget.network("localhost")
                ).databaseName("application=other").build()
        );
    }

    @Test
    void redactsThePasswordFromDiagnostics() {
        DatabaseConfig config = DatabaseConfig.builder(
                        DatabaseType.POSTGRESQL,
                        DatabaseTarget.network("localhost")
                )
                .databaseName("application")
                .username("user")
                .password("secret-value")
                .build();

        assertFalse(config.toString().contains("secret-value"));
    }

    @Test
    void readsTypedSettingsFromTheConfigurationReader() {
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        entries.put(DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("h2"));
        entries.put(DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"));
        entries.put(DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("test"));
        entries.put(DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of("MODE=PostgreSQL")));
        ConfigReader reader = new ScopedLayeredConfigReader(
                ConfigSourceScope.BOOTSTRAP,
                List.of(new TestSource(entries))
        );

        DatabaseConfig config = new DatabaseConfigBinder().bind(reader);

        assertEquals(DatabaseType.H2, config.getType());
        assertEquals(DatabaseTarget.memory(), config.getTarget());
        assertEquals("test", config.getDatabaseName());
        assertEquals("PostgreSQL", config.getDriverProperties().get("MODE"));
        assertEquals(10, config.getPool().getMaximumPoolSize());
    }

    @Test
    void rejectsRuntimeReaderForBootstrapSettings() {
        ConfigReader reader = new ScopedLayeredConfigReader(
                ConfigSourceScope.RUNTIME,
                List.of(new TestSource(Map.of(), ConfigSourceScope.RUNTIME))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new DatabaseConfigBinder().bind(reader)
        );
    }

    @Test
    void readsFileAndInlineSslMaterialDescriptors() {
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        entries.put(DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("postgresql"));
        entries.put(DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("network:localhost:5432"));
        entries.put(DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("application"));
        entries.put(DatabaseConfigSpec.SSL_MODE.bind().getKey(), ConfigEntry.present("verify-ca"));
        entries.put(
                DatabaseConfigSpec.SSL_SERVER_CA.bind().getKey(),
                ConfigEntry.present("value:-----BEGIN CERTIFICATE-----\\ncontent")
        );
        entries.put(DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of()));

        DatabaseConfig config = new DatabaseConfigBinder().bind(new ScopedLayeredConfigReader(
                ConfigSourceScope.BOOTSTRAP,
                List.of(new TestSource(entries))
        ));

        assertEquals(SslMaterial.Source.VALUE, config.getSsl().getServerCaCertificate().getSource());
        assertEquals(
                "-----BEGIN CERTIFICATE-----\\ncontent",
                config.getSsl().getServerCaCertificate().getValue()
        );
    }

    @Test
    void definesDescriptionsForEveryBuiltInSetting() {
        for (Locale locale : List.of(Locale.US, Locale.SIMPLIFIED_CHINESE)) {
            assertFalse(DatabaseConfigSpec.all().stream()
                    .map(spec -> spec.getDescription().resolve(locale))
                    .anyMatch(String::isBlank));
        }
    }

    private static final class TestSource implements ConfigSource {
        private final Map<ConfigKey, ConfigEntry> entries;
        private final ConfigSourceScope scope;

        private TestSource(Map<ConfigKey, ConfigEntry> entries) {
            this(entries, ConfigSourceScope.BOOTSTRAP);
        }

        private TestSource(Map<ConfigKey, ConfigEntry> entries, ConfigSourceScope scope) {
            this.entries = Map.copyOf(entries);
            this.scope = scope;
        }

        @Override
        public ConfigSourceId getId() {
            return ConfigSourceId.of("test");
        }

        @Override
        public ConfigSourceScope getScope() {
            return scope;
        }

        @Override
        public ConfigEntry get(@NonNull ConfigKey key) {
            return entries.getOrDefault(key, ConfigEntry.absent());
        }
    }
}
