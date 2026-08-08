package com.lamprism.luxspec.config;

import com.lamprism.luxspec.ErrorCode;

/**
 * Defines stable configuration-domain error identifiers.
 *
 * @author RollW
 */
public enum ConfigErrorCode implements ErrorCode {
    /**
     * The source value cannot be decoded for the requested type.
     */
    INVALID_VALUE("config:invalid-value"),
    /**
     * A source failed while applying a typed write.
     */
    SOURCE_WRITE_FAILED("config:source-write-failed");

    private final String code;

    ConfigErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
