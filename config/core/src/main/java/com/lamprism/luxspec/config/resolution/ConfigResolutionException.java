package com.lamprism.luxspec.config.resolution;

import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.config.ConfigErrorCode;

/**
 * Indicates that a usable source entry cannot satisfy a configuration definition.
 *
 * @author RollW
 */
public final class ConfigResolutionException extends LuxspecException {
    /**
     * Creates a value-free configuration resolution failure.
     *
     * <p>The exception deliberately omits an underlying codec or validator cause because those
     * failures may contain raw or decoded sensitive configuration values.</p>
     */
    public ConfigResolutionException() {
        super(ConfigErrorCode.INVALID_VALUE, "Configuration value is invalid");
    }
}
