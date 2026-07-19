package com.lamprism.luxspec.config;

/**
 * Describes one operation or storage property supported by a configuration source.
 *
 * @author RollW
 */
public enum ConfigSourceCapability {
    READ,
    WRITE,
    MASK,
    ENUMERATE,
    WATCH,
    PERSISTENT,
    SECURE
}
