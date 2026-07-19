package com.lamprism.luxspec.user.security;

import com.lamprism.luxspec.ErrorCode;

/**
 * Identifies failures while processing protected password representations.
 *
 * @author RollW
 */
public enum PasswordErrorCode implements ErrorCode {
    /**
     * A stored password representation is malformed.
     */
    MALFORMED_ENCODING("user:password-encoding-malformed"),
    /**
     * A stored password representation uses an unsupported protection scheme.
     */
    UNSUPPORTED_ENCODING("user:password-encoding-unsupported"),
    /**
     * The configured password infrastructure could not process a valid representation.
     */
    INFRASTRUCTURE_FAILURE("user:password-infrastructure-failure");

    private final String code;

    PasswordErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
