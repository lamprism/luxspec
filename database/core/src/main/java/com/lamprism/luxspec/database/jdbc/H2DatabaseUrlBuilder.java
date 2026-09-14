package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslMode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds H2 JDBC connection details.
 *
 * @author RollW
 */
public class H2DatabaseUrlBuilder extends BuiltInDatabaseUrlBuilder {
    private static final String DEFAULT_MEMORY_NAME = "luxspec";

    public H2DatabaseUrlBuilder() {
        super(DatabaseType.H2, "org.h2.Driver");
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = settings.getTarget();
        return switch (target.getKind()) {
            case MEMORY -> {
                requireSslDisabled(settings.getSsl());
                yield "jdbc:h2:mem:"
                        + memoryName(settings)
                        + ";DB_CLOSE_DELAY=-1";
            }
            case FILE -> {
                requireSslDisabled(settings.getSsl());
                yield "jdbc:h2:file:" + filePath(target);
            }
            case NETWORK -> networkUrl(settings, target);
        };
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = driverProperties(settings);
        rejectManagedSslOptions(properties, Set.of("SSL"));
        return properties;
    }

    private String networkUrl(DatabaseConfig settings, DatabaseTarget target) {
        SslMode mode = settings.getSsl().getMode();
        if (mode == SslMode.DISABLED) {
            return networkUrl("tcp", settings, target);
        }
        if (mode != SslMode.REQUIRED) {
            throw new IllegalArgumentException(
                    "H2 network SSL supports only required mode in the standard resolver"
            );
        }
        if (hasMaterial(settings.getSsl())) {
            throw new IllegalArgumentException(
                    "H2 SSL material requires a driver-specific configuration"
            );
        }
        return networkUrl("ssl", settings, target);
    }

    private String memoryName(DatabaseConfig settings) {
        String databaseName = settings.getDatabaseName();
        return databaseName == null ? DEFAULT_MEMORY_NAME : databaseName;
    }

    private String filePath(DatabaseTarget target) {
        String value = requireFile(target);
        return value.endsWith(".db")
                ? value.substring(0, value.length() - ".db".length())
                : value;
    }

    private String networkUrl(
            String protocol,
            DatabaseConfig settings,
            DatabaseTarget target
    ) {
        return "jdbc:h2:" + protocol + "://"
                + formatHost(target.getHost())
                + formatPort(target.getPort())
                + "/"
                + requireDatabaseName(settings);
    }
}
