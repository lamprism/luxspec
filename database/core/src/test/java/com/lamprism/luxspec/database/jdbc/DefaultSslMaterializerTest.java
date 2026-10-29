/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 *
 * @author RollW
 */
class DefaultSslMaterializerTest {
    @Test
    void materializesCertificateAndPrivateKeyIntoTemporaryPkcs12Stores() throws Exception {
        SslMaterializer materializer = new DefaultSslMaterializer();
        KeyStoreArtifact trustStore = materializer.materializeTrustStore(
                "test-ca",
                SslMaterial.file(resourcePath("mysql/client-cert.pem"))
        );
        KeyStoreArtifact clientStore = materializer.materializeClientKeyStore(
                "test-client",
                SslMaterial.file(resourcePath("mysql/client-cert.pem")),
                SslMaterial.file(resourcePath("mysql/client-key.pem"))
        );
        Path trustStorePath = trustStore.getPath();
        Path clientStorePath = clientStore.getPath();
        try {
            assertEquals(1, load(trustStore).size());
            KeyStore loadedClientStore = load(clientStore);
            assertTrue(loadedClientStore.isKeyEntry("client"));
        } finally {
            clientStore.close();
            trustStore.close();
        }

        assertFalse(Files.exists(trustStorePath));
        assertFalse(Files.exists(clientStorePath));
    }

    private static KeyStore load(KeyStoreArtifact artifact) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(artifact.getKeyStoreType());
        try (InputStream input = Files.newInputStream(artifact.getPath())) {
            keyStore.load(input, artifact.getPassword().toCharArray());
        }
        return keyStore;
    }

    private static Path resourcePath(String name) throws Exception {
        return Path.of(Objects.requireNonNull(
                DefaultSslMaterializerTest.class.getClassLoader().getResource(name)
        ).toURI());
    }
}
