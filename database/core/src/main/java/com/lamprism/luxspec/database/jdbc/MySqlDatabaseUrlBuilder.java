package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds MySQL JDBC connection details.
 *
 * @author RollW
 */
public class MySqlDatabaseUrlBuilder extends AbstractDatabaseUrlBuilder {
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = requireNetworkTarget(settings);
        return "jdbc:mysql://"
                + formatHost(target.getHost())
                + formatPort(target.getPort())
                + "/"
                + requireDatabaseName(settings);
    }

    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = baseProperties(settings, CharacterSetFlavor.MYSQL);
        rejectManagedSslOptions(properties, Set.of(
                "sslMode",
                "trustCertificateKeyStoreUrl",
                "trustCertificateKeyStoreType",
                "trustCertificateKeyStorePassword",
                "clientCertificateKeyStoreUrl",
                "clientCertificateKeyStoreType",
                "clientCertificateKeyStorePassword"
        ));
        if (hasMaterial(settings.getSsl())) {
            throw new IllegalArgumentException(
                    "MySQL SSL material requires a driver-specific key store adapter"
            );
        }
        properties.put("sslMode", mysqlSslMode(settings.getSsl().getMode()));
        return properties;
    }
}
