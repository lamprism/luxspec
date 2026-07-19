package com.lamprism.luxspec;

/**
 * Common errors used by provider-independent foundation contracts.
 *
 * @author RollW
 */
public enum CommonErrorCode implements ErrorCode {
    /** An unexpected internal failure occurred. */
    INTERNAL("common:internal"),
    /** A caller supplied an invalid argument. */
    INVALID_ARGUMENT("common:invalid-argument"),
    /** The requested operation is invalid for the current state. */
    ILLEGAL_STATE("common:illegal-state"),
    /** A required domain object could not be found. */
    NOT_FOUND("common:not-found"),
    /** The requested capability is not implemented or available. */
    UNSUPPORTED_OPERATION("common:unsupported-operation");

    private final String code;

    CommonErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
