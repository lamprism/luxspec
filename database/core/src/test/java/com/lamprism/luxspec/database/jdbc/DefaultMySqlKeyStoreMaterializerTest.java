package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.SslMaterial;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMySqlKeyStoreMaterializerTest {
    @Test
    void materializesCertificateAndPrivateKeyIntoTemporaryPkcs12Stores() throws Exception {
        DefaultMySqlKeyStoreMaterializer materializer = new DefaultMySqlKeyStoreMaterializer();
        MySqlKeyStoreArtifact trustStore = materializer.materializeTrustStore(
                "test-ca",
                SslMaterial.file(resourcePath("mysql/client-cert.pem"))
        );
        MySqlKeyStoreArtifact clientStore = materializer.materializeClientKeyStore(
                "test-client",
                SslMaterial.file(resourcePath("mysql/client-cert.pem")),
                SslMaterial.file(resourcePath("mysql/client-key.pem"))
        );
        Path trustStorePath = trustStore.getPath();
        Path clientStorePath = clientStore.getPath();
        try {
            assertEquals(1, load(trustStore).size());
            KeyStore loadedClientStore = load(clientStore);
            assertTrue(loadedClientStore.isKeyEntry("mysql-client"));
        } finally {
            clientStore.close();
            trustStore.close();
        }

        assertFalse(Files.exists(trustStorePath));
        assertFalse(Files.exists(clientStorePath));
    }

    private static KeyStore load(MySqlKeyStoreArtifact artifact) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(artifact.getKeyStoreType());
        try (InputStream input = Files.newInputStream(artifact.getPath())) {
            keyStore.load(input, artifact.getPassword().toCharArray());
        }
        return keyStore;
    }

    private static Path resourcePath(String name) throws Exception {
        return Path.of(Objects.requireNonNull(
                DefaultMySqlKeyStoreMaterializerTest.class.getClassLoader().getResource(name)
        ).toURI());
    }
}
