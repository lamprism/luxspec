package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.SslMaterial;

/**
 * Materializes database SSL content into a driver-readable file artifact.
 *
 * <p>The default implementation handles PEM files, inline material, and Java key stores for
 * certificate and client identity material.</p>
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

    /**
     * Materializes a server CA certificate or certificate chain as a trust store.
     *
     * @param name             a non-sensitive artifact name used for temporary file naming
     * @param serverCaMaterial the server CA certificate material
     * @return the temporary trust store artifact
     */
    KeyStoreArtifact materializeTrustStore(String name, SslMaterial serverCaMaterial);

    /**
     * Materializes a client certificate and private key as a key store.
     *
     * @param name              a non-sensitive artifact name used for temporary file naming
     * @param clientCertificate the client certificate material
     * @param clientPrivateKey  the client private key material
     * @return the temporary client key store artifact
     */
    KeyStoreArtifact materializeClientKeyStore(
            String name,
            SslMaterial clientCertificate,
            SslMaterial clientPrivateKey
    );
}
