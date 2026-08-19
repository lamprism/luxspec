package com.lamprism.luxspec.config.resolution;

import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.config.ConfigErrorCode;

/**
 * Indicates that a usable source entry cannot satisfy a configuration definition.
 *
 * @author RollW
 */
public final class ConfigResolutionException extends LuxspecException {
    public ConfigResolutionException() {
        this("Configuration value is invalid");
    }

    public ConfigResolutionException(Throwable cause) {
        this("Configuration value is invalid", cause);
    }

    public ConfigResolutionException(String message) {
        super(ConfigErrorCode.INVALID_VALUE, message);
    }

    public ConfigResolutionException(String message, Throwable cause) {
        super(ConfigErrorCode.INVALID_VALUE, message, cause);
    }
}
