package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslMode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds Oracle Thin JDBC connection details.
 *
 * @author RollW
 */
public class OracleDatabaseUrlBuilder extends AbstractDatabaseUrlBuilder {
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = requireNetworkTarget(settings);
        SslMode mode = settings.getSsl().getMode();
        String url = mode == SslMode.DISABLED
                ? "jdbc:oracle:thin:@//"
                : "jdbc:oracle:thin:@tcps://";
        return url
                + formatHost(target.getHost())
                + formatPort(target.getPort())
                + "/"
                + requireDatabaseName(settings);
    }

    @Override
    protected String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = baseProperties(settings, CharacterSetFlavor.ORACLE);
        rejectManagedSslOptions(
                properties,
                Set.of("oracle.net.ssl_server_dn_match")
        );
        SslMode mode = settings.getSsl().getMode();
        if (mode == SslMode.VERIFY_CA) {
            throw new IllegalArgumentException(
                    "Oracle verify-ca requires a driver-specific trust store adapter"
            );
        }
        if (hasMaterial(settings.getSsl())) {
            throw new IllegalArgumentException(
                    "Oracle SSL material requires driver-specific configuration"
            );
        }
        if (mode != SslMode.DISABLED) {
            properties.put(
                    "oracle.net.ssl_server_dn_match",
                    Boolean.toString(mode == SslMode.VERIFY_IDENTITY)
            );
        }
        return properties;
    }
}
