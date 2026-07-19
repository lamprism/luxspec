package com.lamprism.luxspec.config;

import com.lamprism.luxspec.ErrorCode;

/**
 * Defines stable configuration-domain error identifiers.
 *
 * @author RollW
 */
public enum ConfigErrorCode implements ErrorCode {
    INVALID_VALUE("config:invalid-value");

    private final String code;

    ConfigErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
