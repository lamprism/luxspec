package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslMode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds MariaDB Connector/J connection details.
 *
 * @author RollW
 */
public class MariaDbDatabaseUrlBuilder extends BuiltInDatabaseUrlBuilder {
    private final SslMaterializer sslMaterializer;

    public MariaDbDatabaseUrlBuilder() {
        this(new DefaultSslMaterializer());
    }

    public MariaDbDatabaseUrlBuilder(SslMaterializer sslMaterializer) {
        super(DatabaseType.MARIADB, "org.mariadb.jdbc.Driver");
        this.sslMaterializer = Objects.requireNonNull(sslMaterializer, "sslMaterializer");
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = requireNetworkTarget(settings);
        return "jdbc:mariadb://"
                + formatHost(target.getHost())
                + formatPort(target.getPort())
                + "/"
                + requireDatabaseName(settings);
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = driverProperties(settings);
        applyCharacterSet(properties, settings.getCharset());
        rejectManagedSslOptions(properties, Set.of(
                "sslMode",
                "serverSslCert",
                "fallbackToSystemTrustStore",
                "keyStore",
                "keyStorePassword",
                "keyPassword"
        ));
        if (settings.getSsl().getClientCertificate() != null
                || settings.getSsl().getClientPrivateKey() != null) {
            throw new IllegalArgumentException(
                    "MariaDB client SSL material requires a driver-specific key store adapter"
            );
        }
        properties.put("sslMode", sslMode(settings.getSsl().getMode()));
        putMaterial(
                sslMaterializer,
                properties,
                resources,
                "serverSslCert",
                "mariadb-ca",
                settings.getSsl().getServerCaCertificate()
        );
        if (settings.getSsl().getServerCaCertificate() != null) {
            properties.put("fallbackToSystemTrustStore", "false");
        }
        return properties;
    }

    private void applyCharacterSet(
            Map<String, String> properties,
            @Nullable String characterSet
    ) {
        if (characterSet == null) {
            return;
        }
        rejectManagedOptions(
                properties,
                Set.of("characterEncoding", "useUnicode"),
                "character set"
        );
        properties.put("characterEncoding", characterSet);
        properties.put("useUnicode", "true");
    }

    private String sslMode(SslMode mode) {
        return switch (mode) {
            case DISABLED -> "disable";
            case REQUIRED -> "trust";
            case VERIFY_CA -> "verify-ca";
            case VERIFY_IDENTITY -> "verify-full";
        };
    }
}
