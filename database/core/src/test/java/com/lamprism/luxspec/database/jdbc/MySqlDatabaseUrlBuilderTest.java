package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslConfig;
import com.lamprism.luxspec.database.SslMaterial;
import com.lamprism.luxspec.database.SslMode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlDatabaseUrlBuilderTest {
    @Test
    void mapsMaterializedKeyStoresToConnectorPropertiesAndCleansThemUp() throws Exception {
        AtomicBoolean trustStoreClosed = new AtomicBoolean();
        AtomicBoolean clientStoreClosed = new AtomicBoolean();
        MySqlKeyStoreArtifact trustStore = artifact(
                "truststore.p12",
                "trust-password",
                trustStoreClosed
        );
        MySqlKeyStoreArtifact clientStore = artifact(
                "clientstore.p12",
                "client-password",
                clientStoreClosed
        );
        MySqlDatabaseUrlBuilder builder = new MySqlDatabaseUrlBuilder(
                new StubMySqlKeyStoreMaterializer(trustStore, clientStore)
        );

        JdbcConnectionDetail detail = builder.build(DatabaseConfig.builder(
                        DatabaseType.MYSQL,
                        DatabaseTarget.network("db.internal", 3306)
                )
                .databaseName("application")
                .ssl(SslConfig.builder()
                        .mode(SslMode.VERIFY_IDENTITY)
                        .serverCaCertificate(SslMaterial.value("ca"))
                        .clientCertificate(SslMaterial.value("certificate"))
                        .clientPrivateKey(SslMaterial.value("private-key"))
                        .build())
                .build());

        assertEquals("VERIFY_IDENTITY", detail.getDriverProperties().get("sslMode"));
        assertEquals("PKCS12", detail.getDriverProperties().get("trustCertificateKeyStoreType"));
        assertEquals("trust-password", detail.getDriverProperties().get("trustCertificateKeyStorePassword"));
        assertEquals("PKCS12", detail.getDriverProperties().get("clientCertificateKeyStoreType"));
        assertEquals("client-password", detail.getDriverProperties().get("clientCertificateKeyStorePassword"));
        assertEquals("false", detail.getDriverProperties().get("fallbackToSystemTrustStore"));
        assertEquals("false", detail.getDriverProperties().get("fallbackToSystemKeyStore"));
        assertTrue(detail.getDriverProperties().get("trustCertificateKeyStoreUrl").endsWith("truststore.p12"));
        assertTrue(detail.getDriverProperties().get("clientCertificateKeyStoreUrl").endsWith("clientstore.p12"));

        detail.close();

        assertTrue(trustStoreClosed.get());
        assertTrue(clientStoreClosed.get());
    }

    @Test
    void requiresCaMaterialForVerificationModes() {
        MySqlDatabaseUrlBuilder builder = new MySqlDatabaseUrlBuilder(
                new StubMySqlKeyStoreMaterializer(null, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(DatabaseConfig.builder(
                                DatabaseType.MYSQL,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .ssl(SslConfig.builder().mode(SslMode.VERIFY_CA).build())
                        .build())
        );
    }

    private static MySqlKeyStoreArtifact artifact(
            String fileName,
            String password,
            AtomicBoolean closed
    ) {
        return new MySqlKeyStoreArtifact(
                Path.of(fileName),
                "PKCS12",
                password,
                List.of(new TrackingResource(closed))
        );
    }

    private static final class StubMySqlKeyStoreMaterializer implements MySqlKeyStoreMaterializer {
        private final MySqlKeyStoreArtifact trustStore;
        private final MySqlKeyStoreArtifact clientStore;

        private StubMySqlKeyStoreMaterializer(
                MySqlKeyStoreArtifact trustStore,
                MySqlKeyStoreArtifact clientStore
        ) {
            this.trustStore = trustStore;
            this.clientStore = clientStore;
        }

        @Override
        public MySqlKeyStoreArtifact materializeTrustStore(
                String name,
                SslMaterial serverCaMaterial
        ) {
            return trustStore;
        }

        @Override
        public MySqlKeyStoreArtifact materializeClientKeyStore(
                String name,
                SslMaterial clientCertificate,
                SslMaterial clientPrivateKey
        ) {
            return clientStore;
        }
    }

    private static final class TrackingResource implements AutoCloseable {
        private final AtomicBoolean closed;

        private TrackingResource(AtomicBoolean closed) {
            this.closed = closed;
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }
}
