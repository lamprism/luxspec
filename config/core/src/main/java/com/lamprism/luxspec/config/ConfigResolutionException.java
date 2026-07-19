package com.lamprism.luxspec.config;

import com.lamprism.luxspec.LuxspecException;
import org.jspecify.annotations.Nullable;

/**
 * Indicates that a usable source entry cannot satisfy a configuration definition.
 *
 * @author RollW
 */
public final class ConfigResolutionException extends LuxspecException {
    /**
     * Creates an exception for an invalid source entry or failed codec conversion.
     *
     * @param key the affected complete key
     * @param sourceId the source that produced the invalid entry
     * @param cause the optional conversion failure
     */
    public ConfigResolutionException(ConfigKey key, ConfigSourceId sourceId, @Nullable Throwable cause) {
        super(
                ConfigErrorCode.INVALID_VALUE,
                "Configuration value is invalid for key: " + key.getValue() + " in source: " + sourceId.getValue(),
                cause
        );
    }
}
