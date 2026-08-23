package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.SslMaterial;

/**
 * Creates Java key stores for MySQL Connector/J SSL properties.
 *
 * @author RollW
 */
// TODO: need merge with SslMaterializer
public interface MySqlKeyStoreMaterializer {
    /**
     * Creates a trust store containing a server CA certificate or certificate chain.
     *
     * @param name             a non-sensitive artifact name used for temporary file naming
     * @param serverCaMaterial the server CA certificate material
     * @return the temporary trust store artifact
     */
    MySqlKeyStoreArtifact materializeTrustStore(String name, SslMaterial serverCaMaterial);

    /**
     * Creates a key store containing a client private key and certificate chain.
     *
     * @param name              a non-sensitive artifact name used for temporary file naming
     * @param clientCertificate the client certificate material
     * @param clientPrivateKey  the client private key material
     * @return the temporary client key store artifact
     */
    MySqlKeyStoreArtifact materializeClientKeyStore(
            String name,
            SslMaterial clientCertificate,
            SslMaterial clientPrivateKey
    );
}
