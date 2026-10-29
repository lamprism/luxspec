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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Driver-readable key store artifact and its connection-specific credentials.
 *
 * @author RollW
 */
public final class KeyStoreArtifact implements SslMaterialArtifact {
    private final ManagedSslMaterialArtifact materialArtifact;
    private final String keyStoreType;
    private final String password;

    /**
     * Creates a key store artifact.
     *
     * @param path         the driver-readable key store path
     * @param keyStoreType the Java key store type
     * @param password     the key store password
     * @param resources    resources to close in reverse order
     */
    public KeyStoreArtifact(
            Path path,
            String keyStoreType,
            String password,
            List<? extends AutoCloseable> resources
    ) {
        this.materialArtifact = new ManagedSslMaterialArtifact(path, resources);
        this.keyStoreType = requireText(keyStoreType, "keyStoreType");
        this.password = requireText(password, "password");
    }

    @Override
    public Path getPath() {
        return materialArtifact.getPath();
    }

    /**
     * Returns the Java key store type.
     *
     * @return the key store type
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }

    /**
     * Returns the key store password used for this connection.
     *
     * @return the key store password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public void close() throws Exception {
        materialArtifact.close();
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name).trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
