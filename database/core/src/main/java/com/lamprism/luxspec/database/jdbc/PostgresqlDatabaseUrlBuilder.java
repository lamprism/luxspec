package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslConfig;
import com.lamprism.luxspec.database.SslMode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds PostgreSQL JDBC connection details.
 *
 * @author RollW
 */
public class PostgresqlDatabaseUrlBuilder extends BuiltInDatabaseUrlBuilder {
    private final SslMaterializer sslMaterializer;

    public PostgresqlDatabaseUrlBuilder() {
        this(new DefaultSslMaterializer());
    }

    public PostgresqlDatabaseUrlBuilder(SslMaterializer sslMaterializer) {
        super(DatabaseType.POSTGRESQL, "org.postgresql.Driver");
        this.sslMaterializer = Objects.requireNonNull(sslMaterializer, "sslMaterializer");
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = requireNetworkTarget(settings);
        return "jdbc:postgresql://"
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
                "sslmode",
                "sslrootcert",
                "sslcert",
                "sslkey"
        ));
        SslConfig ssl = settings.getSsl();
        requireCaForVerification(ssl);
        properties.put("sslmode", sslMode(ssl.getMode()));
        putMaterial(
                sslMaterializer,
                properties,
                resources,
                "sslrootcert",
                "postgres-ca",
                ssl.getServerCaCertificate()
        );
        putMaterial(
                sslMaterializer,
                properties,
                resources,
                "sslcert",
                "postgres-cert",
                ssl.getClientCertificate()
        );
        putMaterial(
                sslMaterializer,
                properties,
                resources,
                "sslkey",
                "postgres-key",
                ssl.getClientPrivateKey()
        );
        return properties;
    }

    private static void requireCaForVerification(SslConfig ssl) {
        if ((ssl.getMode() == SslMode.VERIFY_CA || ssl.getMode() == SslMode.VERIFY_IDENTITY)
                && ssl.getServerCaCertificate() == null) {
            throw new IllegalArgumentException(
                    "PostgreSQL SSL verification requires a server CA certificate"
            );
        }
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
        properties.put(
                "characterEncoding",
                utf8 ? "UTF8" : characterSet.toUpperCase(Locale.ROOT)
        );
    }

    private String sslMode(SslMode mode) {
        return switch (mode) {
            case DISABLED -> "disable";
            case REQUIRED -> "require";
            case VERIFY_CA -> "verify-ca";
            case VERIFY_IDENTITY -> "verify-full";
        };
    }
}
