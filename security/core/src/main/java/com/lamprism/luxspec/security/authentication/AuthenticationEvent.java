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

package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.event.Event;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Reports one authentication attempt without retaining the supplied credentials.
 *
 * <p>A successful event contains only the authenticated subject. A failed event contains the
 * precise internal error code and never contains a password, Token, or credential object.</p>
 *
 * @author RollW
 */
public final class AuthenticationEvent implements Event {
    private final String credentialType;
    private final @Nullable Subject subject;
    private final @Nullable ErrorCode errorCode;
    private final Instant occurredAt;
    private final Duration duration;

    private AuthenticationEvent(
            String credentialType,
            @Nullable Subject subject,
            @Nullable ErrorCode errorCode,
            Instant occurredAt,
            Duration duration
    ) {
        this.credentialType = requireText(credentialType, "credentialType");
        if ((subject == null) == (errorCode == null)) {
            throw new IllegalArgumentException("Exactly one authentication result must be supplied");
        }
        this.subject = subject;
        this.errorCode = errorCode;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        Duration nonNullDuration = Objects.requireNonNull(duration, "duration");
        if (nonNullDuration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        this.duration = nonNullDuration;
    }

    /**
     * Creates a successful authentication event.
     *
     * @param credentialType the stable credential type name
     * @param authentication the successful authentication result
     * @param occurredAt     the completion time
     * @param duration       the elapsed authentication duration
     * @return the immutable success event
     */
    public static AuthenticationEvent succeeded(
            String credentialType,
            Authentication authentication,
            Instant occurredAt,
            Duration duration
    ) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        return new AuthenticationEvent(
                credentialType,
                nonNullAuthentication.subject(),
                null,
                occurredAt,
                duration
        );
    }

    /**
     * Creates a failed authentication event.
     *
     * @param credentialType the stable credential type name
     * @param errorCode      the precise internal failure code
     * @param occurredAt     the completion time
     * @param duration       the elapsed authentication duration
     * @return the immutable failure event
     */
    public static AuthenticationEvent failed(
            String credentialType,
            ErrorCode errorCode,
            Instant occurredAt,
            Duration duration
    ) {
        return new AuthenticationEvent(
                credentialType,
                null,
                Objects.requireNonNull(errorCode, "errorCode"),
                occurredAt,
                duration
        );
    }

    /**
     * Reports whether authentication succeeded.
     *
     * @return {@code true} for a successful authentication
     */
    public boolean isSuccessful() {
        return subject != null;
    }

    /**
     * Returns the stable credential type name.
     *
     * @return the credential type name
     */
    public String getCredentialType() {
        return credentialType;
    }

    /**
     * Returns the authenticated subject when authentication succeeded.
     *
     * @return the subject, or {@code null} after a failed attempt
     */
    public @Nullable Subject getSubject() {
        return subject;
    }

    /**
     * Returns the precise failure code when authentication failed.
     *
     * @return the error code, or {@code null} after a successful attempt
     */
    public @Nullable ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the attempt completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Returns the elapsed authentication duration.
     *
     * @return the elapsed duration
     */
    public Duration getDuration() {
        return duration;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
