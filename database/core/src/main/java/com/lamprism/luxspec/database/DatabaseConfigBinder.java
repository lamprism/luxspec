package com.lamprism.luxspec.database;

import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Binds bootstrap configuration definitions to immutable database settings.
 */
public class DatabaseConfigBinder {
    /**
     * Binds database settings from a bootstrap-scoped configuration reader.
     *
     * @param reader the bootstrap configuration reader
     * @return the validated database settings
     */
    public DatabaseConfig bind(ConfigReader reader) {
        ConfigReader nonNullReader = requireBootstrapReader(reader);
        DatabaseConfig.Builder builder = DatabaseConfig.builder(
                required(nonNullReader, DatabaseConfigSpec.TYPE),
                required(nonNullReader, DatabaseConfigSpec.TARGET)
        );
        builder.databaseName(optional(nonNullReader, DatabaseConfigSpec.NAME));
        builder.username(optional(nonNullReader, DatabaseConfigSpec.USERNAME));
        builder.password(optional(nonNullReader, DatabaseConfigSpec.PASSWORD));
        builder.characterSet(optional(nonNullReader, DatabaseConfigSpec.CHARACTER_SET));
        builder.properties(parseProperties(required(nonNullReader, DatabaseConfigSpec.OPTIONS)));
        builder.ssl(readSsl(nonNullReader));
        builder.pool(readPool(nonNullReader));
        return builder.build();
    }

    private static ConfigReader requireBootstrapReader(ConfigReader reader) {
        ConfigReader nonNullReader = Objects.requireNonNull(reader, "reader");
        if (nonNullReader.getSourceScope() != ConfigSourceScope.BOOTSTRAP) {
            throw new IllegalArgumentException("Database settings require a bootstrap ConfigReader");
        }
        return nonNullReader;
    }

    private static SslConfig readSsl(ConfigReader reader) {
        SslConfig.Builder builder = SslConfig.builder()
                .mode(required(reader, DatabaseConfigSpec.SSL_MODE));
        builder.serverCaCertificate(toMaterial(optional(reader, DatabaseConfigSpec.SSL_SERVER_CA)));
        builder.clientCertificate(toMaterial(optional(reader, DatabaseConfigSpec.SSL_CLIENT_CERTIFICATE)));
        builder.clientPrivateKey(toMaterial(optional(reader, DatabaseConfigSpec.SSL_CLIENT_PRIVATE_KEY)));
        return builder.build();
    }

    private static ConnectionPoolPolicy readPool(ConfigReader reader) {
        return ConnectionPoolPolicy.builder()
                .maximumPoolSize(required(reader, DatabaseConfigSpec.POOL_MAXIMUM_SIZE))
                .minimumIdle(required(reader, DatabaseConfigSpec.POOL_MINIMUM_IDLE))
                .connectionTimeout(required(reader, DatabaseConfigSpec.POOL_CONNECTION_TIMEOUT))
                .idleTimeout(required(reader, DatabaseConfigSpec.POOL_IDLE_TIMEOUT))
                .maximumLifetime(required(reader, DatabaseConfigSpec.POOL_MAXIMUM_LIFETIME))
                .leakDetectionThreshold(required(reader, DatabaseConfigSpec.POOL_LEAK_DETECTION_THRESHOLD))
                .build();
    }

    private static Map<String, String> parseProperties(List<String> entries) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (String entry : entries) {
            int separator = entry.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("database.options entries must use key=value format");
            }
            String key = entry.substring(0, separator).trim();
            String value = entry.substring(separator + 1);
            if (key.isEmpty()) {
                throw new IllegalArgumentException("database.options property key must not be blank");
            }
            if (properties.put(key, value) != null) {
                throw new IllegalArgumentException("database.options contains a duplicate property key");
            }
        }
        return Map.copyOf(properties);
    }

    private static SslMaterial toMaterial(@Nullable String value) {
        return value == null ? null : SslMaterial.parse(value);
    }

    private static <T> T required(ConfigReader reader, ConfigSpec<T> spec) {
        ConfigValue<T> value = reader.get(spec);
        if (!value.hasValue()) {
            throw new IllegalArgumentException("Missing required database setting: " + spec.getKey().getValue());
        }
        return value.requireValue();
    }

    private static <T> @Nullable T optional(ConfigReader reader, ConfigSpec<T> spec) {
        ConfigValue<T> value = reader.get(spec);
        return value.hasValue() ? value.getValue() : null;
    }
}
