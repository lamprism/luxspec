package com.lamprism.luxspec.database.jdbc;

import java.nio.file.Path;

/**
 * Driver-readable SSL file artifact with an explicit cleanup lifecycle.
 *
 * <p>Callers own the returned artifact and must close it after the associated
 * connection configuration no longer needs the file.</p>
 *
 * @author RollW
 */
public interface SslMaterialArtifact extends AutoCloseable {
    /**
     * Returns the driver-readable material path.
     *
     * @return the SSL material path
     */
    Path getPath();
}
