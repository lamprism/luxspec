package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.SslMaterial;

/**
 * Materializes database SSL content into a driver-readable file artifact.
 *
 * <p>Driver adapters may provide a specialized implementation for trust stores or key stores.
 * The default implementation only handles PEM file and inline material.</p>
 *
 * @author RollW
 */
public interface SslMaterializer {
    /**
     * Materializes one SSL value.
     *
     * @param name     a non-sensitive artifact name used for temporary file naming
     * @param material the source material
     * @return the artifact and its cleanup resources
     */
    SslMaterialArtifact materialize(String name, SslMaterial material);
}
