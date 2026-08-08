package com.lamprism.luxspec.config.resolution;

import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.config.ConfigErrorCode;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Objects;

/**
 * Indicates that a usable source entry cannot satisfy a configuration definition.
 *
 * @author RollW
 */
public final class ConfigResolutionException extends LuxspecException {
    /**
     * Creates an exception for an invalid source entry or failed codec conversion.
     *
     * @param key      the affected complete key
     * @param sourceId the source that produced the invalid entry
     * @param detail   a value-free explanation
     */
    public ConfigResolutionException(
            ConfigKey key,
            ConfigSourceId sourceId,
            String detail
    ) {
        super(
                ConfigErrorCode.INVALID_VALUE,
                "Configuration value is invalid for key: " + key.getValue()
                        + " in source: " + sourceId.getValue() + ": " + requireDetail(detail)
        );
    }

    private static String requireDetail(String detail) {
        String nonBlankDetail = Objects.requireNonNull(detail, "detail");
        if (nonBlankDetail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return nonBlankDetail;
    }
}
