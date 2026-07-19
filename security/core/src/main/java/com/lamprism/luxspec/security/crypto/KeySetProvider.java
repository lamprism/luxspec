package com.lamprism.luxspec.security.crypto;

/**
 * Resolves immutable cryptographic key-set snapshots by provider-owned name.
 *
 * @author RollW
 */
@FunctionalInterface
public interface KeySetProvider {
    /**
     * Returns the current snapshot for one key-set name.
     *
     * @param keySetName the provider-local key-set name
     * @return the matching non-null key set
     * @throws IllegalArgumentException when the provider does not define the name
     */
    KeySet get(String keySetName);
}
