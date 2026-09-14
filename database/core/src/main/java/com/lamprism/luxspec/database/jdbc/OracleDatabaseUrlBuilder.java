package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslMode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds Oracle Thin JDBC connection details.
 *
 * @author RollW
 */
public class OracleDatabaseUrlBuilder extends BuiltInDatabaseUrlBuilder {
    public OracleDatabaseUrlBuilder() {
        super(DatabaseType.ORACLE, "oracle.jdbc.OracleDriver");
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
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = driverProperties(settings);
        applyCharacterSet(properties, settings.getCharset());
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

    private void applyCharacterSet(
            Map<String, String> properties,
            @Nullable String characterSet
    ) {
        if (characterSet == null) {
            return;
        }
        Set<String> managedNames = Set.of(
                "oracle.jdbc.defaultNChar",
                "oracle.jdbc.UseNLSProcessing"
        );
        rejectManagedOptions(properties, managedNames, "character set");
        boolean unicode = characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4");
        properties.put("oracle.jdbc.defaultNChar", Boolean.toString(unicode));
        if (unicode) {
            properties.put("oracle.jdbc.UseNLSProcessing", "false");
        }
    }
}
