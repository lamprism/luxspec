package com.lamprism.luxspec.resource;

import com.lamprism.luxspec.ErrorCode;

/**
 * Stable provider-independent resource error identities.
 *
 * @author RollW
 */
public enum ResourceErrorCode implements ErrorCode {
    /** A requested resource does not exist. */
    NOT_FOUND("resource:not-found"),
    /** No provider has been registered for a resource type. */
    PROVIDER_NOT_REGISTERED("resource:provider-not-registered"),
    /** A resource reference violates its provider contract. */
    INVALID_REFERENCE("resource:invalid-reference"),
    /** A provider could not resolve a valid resource reference. */
    RESOLUTION_FAILED("resource:resolution-failed"),
    /** Resource provider registrations conflict. */
    PROVIDER_CONFLICT("resource:provider-conflict");

    private final String code;

    ResourceErrorCode(String code) {
        this.code = code;
    }

    /**
     * Returns the canonical resource error code.
     *
     * @return the error code
     */
    @Override
    public String getCode() {
        return code;
    }
}
