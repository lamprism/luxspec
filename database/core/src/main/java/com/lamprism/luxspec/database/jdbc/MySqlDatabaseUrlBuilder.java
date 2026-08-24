package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslConfig;
import com.lamprism.luxspec.database.SslMaterial;
import com.lamprism.luxspec.database.SslMode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds MySQL JDBC connection details.
 *
 * @author RollW
 */
public class MySqlDatabaseUrlBuilder extends AbstractDatabaseUrlBuilder {
    private final SslMaterializer sslMaterializer;

    public MySqlDatabaseUrlBuilder() {
        this(new DefaultSslMaterializer());
    }

    public MySqlDatabaseUrlBuilder(SslMaterializer sslMaterializer) {
        this.sslMaterializer = Objects.requireNonNull(sslMaterializer, "sslMaterializer");
    }

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
                "clientCertificateKeyStorePassword",
                "fallbackToSystemTrustStore",
                "fallbackToSystemKeyStore"
        ));
        SslConfig ssl = settings.getSsl();
        requireCaForVerification(ssl);
        properties.put("sslMode", mysqlSslMode(ssl.getMode()));
        putTrustStore(properties, resources, ssl.getServerCaCertificate());
        putClientKeyStore(
                properties,
                resources,
                ssl.getClientCertificate(),
                ssl.getClientPrivateKey()
        );
        return properties;
    }

    private void putTrustStore(
            Map<String, String> properties,
            List<AutoCloseable> resources,
            @Nullable SslMaterial serverCaCertificate
    ) {
        if (serverCaCertificate == null) {
            return;
        }
        KeyStoreArtifact artifact = Objects.requireNonNull(
                sslMaterializer.materializeTrustStore("mysql-ca", serverCaCertificate),
                "trust store artifact"
        );
        resources.add(artifact);
        putKeyStoreProperties(
                properties,
                artifact,
                "trustCertificateKeyStoreUrl",
                "trustCertificateKeyStoreType",
                "trustCertificateKeyStorePassword"
        );
        properties.put("fallbackToSystemTrustStore", "false");
    }

    private void putClientKeyStore(
            Map<String, String> properties,
            List<AutoCloseable> resources,
            @Nullable SslMaterial clientCertificate,
            @Nullable SslMaterial clientPrivateKey
    ) {
        if (clientCertificate == null) {
            return;
        }
        KeyStoreArtifact artifact = Objects.requireNonNull(
                sslMaterializer.materializeClientKeyStore(
                        "mysql-client",
                        clientCertificate,
                        Objects.requireNonNull(clientPrivateKey, "clientPrivateKey")
                ),
                "client key store artifact"
        );
        resources.add(artifact);
        putKeyStoreProperties(
                properties,
                artifact,
                "clientCertificateKeyStoreUrl",
                "clientCertificateKeyStoreType",
                "clientCertificateKeyStorePassword"
        );
        properties.put("fallbackToSystemKeyStore", "false");
    }

    private static void putKeyStoreProperties(
            Map<String, String> properties,
            KeyStoreArtifact artifact,
            String urlProperty,
            String typeProperty,
            String passwordProperty
    ) {
        properties.put(
                urlProperty,
                artifact.getPath().toAbsolutePath().normalize().toUri().toString()
        );
        properties.put(typeProperty, artifact.getKeyStoreType());
        properties.put(passwordProperty, artifact.getPassword());
    }

    private static void requireCaForVerification(SslConfig ssl) {
        if ((ssl.getMode() == SslMode.VERIFY_CA || ssl.getMode() == SslMode.VERIFY_IDENTITY)
                && ssl.getServerCaCertificate() == null) {
            throw new IllegalArgumentException(
                    "MySQL SSL verification requires a server CA certificate"
            );
        }
    }
}
