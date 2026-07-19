package com.lamprism.luxspec;

/**
 * Common authentication and authorization errors.
 *
 * @author RollW
 */
public enum AuthErrorCode implements ErrorCode {
    /**
     * An access token could not be verified or is no longer usable.
     */
    INVALID_TOKEN("security:invalid-token"),
    /**
     * A verified access token has been explicitly revoked.
     */
    ACCESS_TOKEN_REVOKED("security:access-token-revoked"),
    /**
     * A refresh token could not be consumed or exchanged.
     */
    REFRESH_TOKEN_REJECTED("security:refresh-token-rejected"),
    /**
     * The authenticated subject does not hold a required authorization grant.
     */
    PERMISSION_DENIED("security:permission-denied"),
    /**
     * No configured authenticator supports the supplied credentials.
     */
    UNSUPPORTED_CREDENTIALS("security:unsupported-credentials"),
    /**
     * Submitted credentials did not identify an authenticated subject.
     */
    INVALID_CREDENTIALS("security:invalid-credentials"),
    /**
     * The authenticated subject does not exist.
     */
    SUBJECT_NOT_FOUND("security:subject-not-found"),
    /**
     * The authenticated subject is disabled.
     */
    SUBJECT_DISABLED("security:subject-disabled"),
    /**
     * The authenticated subject is locked.
     */
    SUBJECT_LOCKED("security:subject-locked"),
    /**
     * The authenticated subject has been canceled.
     */
    SUBJECT_CANCELED("security:subject-canceled");

    private final String code;

    AuthErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
