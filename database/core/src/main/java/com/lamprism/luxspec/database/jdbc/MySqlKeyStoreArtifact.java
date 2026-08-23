package com.lamprism.luxspec.database.jdbc;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * MySQL Connector/J key store file and its connection-specific credentials.
 *
 * @author RollW
 */
public final class MySqlKeyStoreArtifact implements AutoCloseable {
    private final SslMaterialArtifact material;
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
    public MySqlKeyStoreArtifact(
            Path path,
            String keyStoreType,
            String password,
            List<? extends AutoCloseable> resources
    ) {
        this.material = new SslMaterialArtifact(path, resources);
        this.keyStoreType = requireText(keyStoreType, "keyStoreType");
        this.password = requireText(password, "password");
    }

    /**
     * Returns the driver-readable key store path.
     *
     * @return the key store path
     */
    public Path getPath() {
        return material.getPath();
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
        material.close();
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name).trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
