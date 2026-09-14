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
 * Builds Microsoft SQL Server JDBC connection details.
 *
 * @author RollW
 */
public class SqlServerDatabaseUrlBuilder extends BuiltInDatabaseUrlBuilder {
    public SqlServerDatabaseUrlBuilder() {
        super(DatabaseType.SQL_SERVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = requireNetworkTarget(settings);
        return "jdbc:sqlserver://"
                + formatHost(target.getHost())
                + formatPort(target.getPort())
                + ";databaseName="
                + requireDatabaseName(settings);
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        DatabaseTarget target = requireNetworkTarget(settings);
        Map<String, String> properties = driverProperties(settings);
        applyCharacterSet(properties, settings.getCharset());
        rejectManagedSslOptions(properties, Set.of(
                "encrypt",
                "trustServerCertificate",
                "hostNameInCertificate",
                "trustStore",
                "trustStoreType",
                "trustStorePassword"
        ));
        SslMode mode = settings.getSsl().getMode();
        if (mode == SslMode.VERIFY_CA) {
            throw new IllegalArgumentException(
                    "SQL Server verify-ca requires a driver-specific trust store adapter"
            );
        }
        if (hasMaterial(settings.getSsl())) {
            throw new IllegalArgumentException(
                    "SQL Server SSL material requires driver-specific configuration"
            );
        }
        properties.put("encrypt", Boolean.toString(mode != SslMode.DISABLED));
        properties.put("trustServerCertificate", Boolean.toString(mode == SslMode.REQUIRED));
        if (mode == SslMode.VERIFY_IDENTITY) {
            properties.put(
                    "hostNameInCertificate",
                    Objects.requireNonNull(target.getHost(), "host")
            );
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
        rejectManagedOptions(properties, Set.of("characterEncoding"), "character set");
        boolean utf8 = characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4");
        properties.put("characterEncoding", utf8 ? "UTF-8" : characterSet);
    }
}
