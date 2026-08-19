package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslConfig;
import com.lamprism.luxspec.database.SslMaterial;
import com.lamprism.luxspec.database.SslMode;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Common policy and lifecycle operations for the built-in database URL builders.
 *
 * <p>The public extension point is {@link DatabaseUrlBuilder}. This base class keeps shared
 * validation, driver property policy, and SSL resource cleanup beside the built-in builder
 * implementations without exposing a generic utility API.</p>
 *
 * @author RollW
 */
public abstract class AbstractDatabaseUrlBuilder implements DatabaseUrlBuilder {
    protected AbstractDatabaseUrlBuilder() {
    }

    @Override
    public JdbcConnectionDetail build(DatabaseConfig settings) {
        DatabaseConfig nonNullSettings = Objects.requireNonNull(settings, "settings");
        requireType(nonNullSettings, getDatabaseType());
        List<AutoCloseable> resources = new ArrayList<>();
        try {
            return new JdbcConnectionDetail(
                    buildJdbcUrl(nonNullSettings),
                    getDriverClassName(),
                    buildDriverProperties(nonNullSettings, resources),
                    resources
            );
        } catch (RuntimeException | Error exception) {
            closeResources(resources, exception);
            throw exception;
        }
    }

    /**
     * Builds the dialect-specific JDBC URL without driver properties.
     *
     * @param settings validated database settings
     * @return the JDBC URL
     */
    protected abstract String buildJdbcUrl(DatabaseConfig settings);

    /**
     * Returns the driver class used by the dialect.
     *
     * @return the driver class name
     */
    protected abstract String getDriverClassName();

    /**
     * Builds driver properties and may register closeable materialization resources.
     *
     * @param settings  validated database settings
     * @param resources resources owned by the resulting connection detail
     * @return driver properties
     */
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        return new LinkedHashMap<>(settings.getDriverProperties());
    }

    protected void requireType(DatabaseConfig settings, DatabaseType expectedType) {
        DatabaseConfig nonNullSettings = Objects.requireNonNull(settings, "settings");
        if (!nonNullSettings.getType().equals(expectedType)) {
            throw new IllegalArgumentException(
                    "Builder does not support database type: " + nonNullSettings.getType()
            );
        }
    }

    protected DatabaseTarget requireNetworkTarget(DatabaseConfig settings) {
        DatabaseTarget target = settings.getTarget();
        if (target.getKind() != DatabaseTarget.Kind.NETWORK) {
            throw new IllegalArgumentException(settings.getType() + " requires a network target");
        }
        return target;
    }

    protected String requireDatabaseName(DatabaseConfig settings) {
        return Objects.requireNonNull(settings.getDatabaseName(), "databaseName");
    }

    protected String memoryName(DatabaseConfig settings, String defaultName) {
        String databaseName = settings.getDatabaseName();
        return databaseName == null ? defaultName : databaseName;
    }

    protected String requireFile(DatabaseTarget target) {
        String value = Objects.requireNonNull(target.getFile(), "file")
                .toAbsolutePath()
                .normalize()
                .toString();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == ';' || character == '?' || character == '#') {
                throw new IllegalArgumentException("file contains a JDBC URL delimiter");
            }
        }
        return value;
    }

    protected String h2FilePath(DatabaseTarget target) {
        String value = requireFile(target);
        return value.endsWith(".db")
                ? value.substring(0, value.length() - ".db".length())
                : value;
    }

    protected String formatHost(@Nullable String host) {
        String value = Objects.requireNonNull(host, "host");
        return value.indexOf(':') >= 0 && !value.startsWith("[")
                ? "[" + value + "]"
                : value;
    }

    protected String formatPort(@Nullable Integer port) {
        return port == null ? "" : ":" + port;
    }

    protected Map<String, String> baseProperties(
            DatabaseConfig settings,
            CharacterSetFlavor characterSetFlavor
    ) {
        Map<String, String> properties = new LinkedHashMap<>(settings.getDriverProperties());
        String characterSet = settings.getCharset();
        if (characterSet == null) {
            return properties;
        }
        rejectManagedOptions(properties, characterSetFlavor.managedProperties(), "character set");
        applyCharacterSet(properties, characterSet, characterSetFlavor);
        return properties;
    }

    protected void requireSslDisabled(SslConfig ssl, DatabaseType type) {
        if (ssl.getMode() != SslMode.DISABLED || hasMaterial(ssl)) {
            throw new IllegalArgumentException(type + " does not support SSL in the standard resolver");
        }
    }

    protected boolean hasMaterial(SslConfig ssl) {
        return ssl.getServerCaCertificate() != null
                || ssl.getClientCertificate() != null
                || ssl.getClientPrivateKey() != null;
    }

    protected void rejectManagedSslOptions(Map<String, String> properties, Set<String> managedNames) {
        rejectManagedOptions(properties, managedNames, "SSL");
    }

    protected void rejectManagedOptions(
            Map<String, String> properties,
            Set<String> managedNames,
            String category
    ) {
        for (String propertyName : properties.keySet()) {
            for (String managedName : managedNames) {
                if (propertyName.equalsIgnoreCase(managedName)) {
                    throw new IllegalArgumentException(
                            "database.options contains a managed " + category
                                    + " property: " + propertyName
                    );
                }
            }
        }
    }

    protected void putMaterial(
            SslMaterializer materializer,
            Map<String, String> properties,
            List<AutoCloseable> resources,
            String propertyName,
            String artifactName,
            @Nullable SslMaterial material
    ) {
        if (material == null) {
            return;
        }
        SslMaterialArtifact artifact = materializer.materialize(artifactName, material);
        resources.add(artifact);
        properties.put(propertyName, artifact.getPath().toString());
    }

    protected void closeResources(List<AutoCloseable> resources, Throwable failure) {
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception cleanupException) {
                failure.addSuppressed(cleanupException);
            }
        }
    }

    protected String postgresSslMode(SslMode mode) {
        return switch (mode) {
            case DISABLED -> "disable";
            case REQUIRED -> "require";
            case VERIFY_CA -> "verify-ca";
            case VERIFY_IDENTITY -> "verify-full";
        };
    }

    protected String mysqlSslMode(SslMode mode) {
        return switch (mode) {
            case DISABLED -> "DISABLED";
            case REQUIRED -> "REQUIRED";
            case VERIFY_CA -> "VERIFY_CA";
            case VERIFY_IDENTITY -> "VERIFY_IDENTITY";
        };
    }

    protected String mariaDbSslMode(SslMode mode) {
        return switch (mode) {
            case DISABLED -> "disable";
            case REQUIRED -> "trust";
            case VERIFY_CA -> "verify-ca";
            case VERIFY_IDENTITY -> "verify-full";
        };
    }

    private void applyCharacterSet(
            Map<String, String> properties,
            String characterSet,
            CharacterSetFlavor flavor
    ) {
        switch (flavor) {
            case MYSQL, MARIADB -> {
                properties.put("characterEncoding", characterSet);
                properties.put("useUnicode", "true");
            }
            case POSTGRESQL -> properties.put("characterEncoding", postgresqlCharacterSet(characterSet));
            case SQLITE -> properties.put("encoding", sqliteCharacterSet(characterSet));
            case SQL_SERVER -> properties.put("characterEncoding", sqlServerCharacterSet(characterSet));
            case ORACLE -> applyOracleCharacterSet(properties, characterSet);
            case H2 -> {
            }
        }
    }

    private String postgresqlCharacterSet(String characterSet) {
        if (characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4")) {
            return "UTF8";
        }
        return characterSet.toUpperCase(Locale.ROOT);
    }

    private String sqliteCharacterSet(String characterSet) {
        if (characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4")) {
            return "UTF-8";
        }
        return characterSet;
    }

    private String sqlServerCharacterSet(String characterSet) {
        if (characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4")) {
            return "UTF-8";
        }
        return characterSet;
    }

    private void applyOracleCharacterSet(
            Map<String, String> properties,
            String characterSet
    ) {
        boolean unicode = characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4");
        properties.put("oracle.jdbc.defaultNChar", Boolean.toString(unicode));
        if (unicode) {
            properties.put("oracle.jdbc.UseNLSProcessing", "false");
        }
    }

    protected enum CharacterSetFlavor {
        H2 {
            @Override
            Set<String> managedProperties() {
                return Set.of();
            }
        },
        MYSQL {
            @Override
            Set<String> managedProperties() {
                return Set.of("characterEncoding", "useUnicode");
            }
        },
        MARIADB {
            @Override
            Set<String> managedProperties() {
                return Set.of("characterEncoding", "useUnicode");
            }
        },
        POSTGRESQL {
            @Override
            Set<String> managedProperties() {
                return Set.of("characterEncoding");
            }
        },
        SQLITE {
            @Override
            Set<String> managedProperties() {
                return Set.of("encoding");
            }
        },
        SQL_SERVER {
            @Override
            Set<String> managedProperties() {
                return Set.of("characterEncoding");
            }
        },
        ORACLE {
            @Override
            Set<String> managedProperties() {
                return Set.of("oracle.jdbc.defaultNChar", "oracle.jdbc.UseNLSProcessing");
            }
        };

        abstract Set<String> managedProperties();
    }
}
