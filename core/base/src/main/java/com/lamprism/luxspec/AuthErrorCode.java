/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * Authentication failed because a trusted backend or authenticator encountered an unexpected failure.
     */
    AUTHENTICATION_FAILURE("security:authentication-failure"),
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
