package com.lamprism.luxspec.database;

/**
 * Database transport security policy.
 */
public enum SslMode {
    DISABLED,
    REQUIRED,
    VERIFY_CA,
    VERIFY_IDENTITY
}
