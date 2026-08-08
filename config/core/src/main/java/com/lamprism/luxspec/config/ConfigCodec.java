package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.RawConfigValue;


/**
 * Converts one configuration value to and from its provider-neutral raw representation.
 *
 * <p>A codec owns representation conversion, not the definition's business constraints. A codec
 * may accept more than one raw scalar kind when those representations have the same meaning for
 * its type. For example, a size codec can accept an integral raw value as bytes and a string raw
 * value such as {@code "1KB"}. This policy is independent of whether the source is TOML, a
 * database, or an environment adapter.</p>
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
     * @throws IllegalArgumentException when the typed value cannot be encoded
     */
    RawConfigValue encode(T value);
}
