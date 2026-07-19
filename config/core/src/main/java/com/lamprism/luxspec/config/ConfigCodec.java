package com.lamprism.luxspec.config;


/**
 * Converts one configuration value to and from its provider-neutral raw representation.
 *
 * @param <T> the typed value
 * @author RollW
 */
public interface ConfigCodec<T> {
    /**
     * Decodes a raw source value.
     *
     * @param rawValue the scalar or list source value
     * @return the decoded value
     * @throws IllegalArgumentException when the raw value is invalid for this codec
     */
    T decode(RawConfigValue rawValue);

    /**
     * Encodes a typed value without losing scalar or list boundaries.
     *
     * @param value the typed value
     * @return the provider-neutral raw value
     */
    RawConfigValue encode(T value);
}
