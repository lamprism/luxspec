package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslConfig;
import com.lamprism.luxspec.database.SslMode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds PostgreSQL JDBC connection details.
 *
 * @author RollW
 */
public class PostgresqlDatabaseUrlBuilder extends AbstractDatabaseUrlBuilder {
    private final SslMaterializer sslMaterializer;

    public PostgresqlDatabaseUrlBuilder() {
        this(new DefaultSslMaterializer());
    }

    public PostgresqlDatabaseUrlBuilder(SslMaterializer sslMaterializer) {
        this.sslMaterializer = Objects.requireNonNull(sslMaterializer, "sslMaterializer");
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
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
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = baseProperties(settings, CharacterSetFlavor.POSTGRESQL);
        rejectManagedSslOptions(properties, Set.of(
                "sslmode",
                "sslrootcert",
                "sslcert",
                "sslkey"
        ));
        SslConfig ssl = settings.getSsl();
        requireCaForVerification(ssl);
        properties.put("sslmode", postgresSslMode(ssl.getMode()));
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
}
