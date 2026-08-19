package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds MariaDB Connector/J connection details.
 *
 * @author RollW
 */
public class MariaDbDatabaseUrlBuilder extends AbstractDatabaseUrlBuilder {
    private final SslMaterializer sslMaterializer;

    public MariaDbDatabaseUrlBuilder() {
        this(new DefaultSslMaterializer());
    }

    public MariaDbDatabaseUrlBuilder(SslMaterializer sslMaterializer) {
        this.sslMaterializer = Objects.requireNonNull(sslMaterializer, "sslMaterializer");
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MARIADB;
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
    protected String getDriverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = baseProperties(settings, CharacterSetFlavor.MARIADB);
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
        properties.put("sslMode", mariaDbSslMode(settings.getSsl().getMode()));
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
}
