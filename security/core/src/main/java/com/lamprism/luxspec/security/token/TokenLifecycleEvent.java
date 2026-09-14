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

package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.event.Event;
import com.lamprism.luxspec.security.authentication.Subject;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Reports a token issue, refresh, or revocation operation without retaining token values or digests.
 *
 * <p>Token kinds are copied as canonical names. Issue and refresh events contain the authenticated
 * subject and issued kinds. Revocation events may omit the subject when the revoking boundary only
 * receives a token or session identifier. Rejected events contain only the operation and stable
 * error code.</p>
 *
 * @author RollW
 */
public final class TokenLifecycleEvent implements Event {
    /**
     * The token operation represented by an event.
     */
    public enum Operation {
        ISSUE,
        REFRESH,
        REVOKE
    }

    /**
     * The result of the token operation.
     */
    public enum Result {
        SUCCESS,
        REJECTED,
        FAILED
    }

    private final Operation operation;
    private final Result result;
    private final Set<String> tokenKinds;
    private final @Nullable Subject subject;
    private final @Nullable ErrorCode errorCode;
    private final Instant occurredAt;
    private final Duration duration;

    private TokenLifecycleEvent(
            Operation operation,
            Result result,
            Set<String> tokenKinds,
            @Nullable Subject subject,
            @Nullable ErrorCode errorCode,
            Instant occurredAt,
            Duration duration
    ) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.result = Objects.requireNonNull(result, "result");
        this.tokenKinds = Set.copyOf(tokenKinds);
        this.subject = subject;
        this.errorCode = errorCode;
        if (result == Result.SUCCESS) {
            if (this.tokenKinds.isEmpty() || errorCode != null) {
                throw new IllegalArgumentException("Successful token events require token kinds and no error code");
            }
            if (operation != Operation.REVOKE && subject == null) {
                throw new IllegalArgumentException("Issue and refresh events require a subject");
            }
        } else if (errorCode == null) {
            throw new IllegalArgumentException("Rejected or failed token events require an error code");
        }
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        Duration nonNullDuration = Objects.requireNonNull(duration, "duration");
        if (nonNullDuration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        this.duration = nonNullDuration;
    }

    /**
     * Creates a successful issue event.
     *
     * @param subject    the authenticated subject
     * @param issuance   the issued token metadata
     * @param occurredAt the completion time
     * @param duration   the elapsed operation duration
     * @return the immutable event
     */
    public static TokenLifecycleEvent issued(
            Subject subject,
            TokenIssuance issuance,
            Instant occurredAt,
            Duration duration
    ) {
        return success(Operation.ISSUE, subject, issuance, occurredAt, duration);
    }

    /**
     * Creates a successful refresh event.
     *
     * @param subject    the authenticated subject
     * @param issuance   the issued successor token metadata
     * @param occurredAt the completion time
     * @param duration   the elapsed operation duration
     * @return the immutable event
     */
    public static TokenLifecycleEvent refreshed(
            Subject subject,
            TokenIssuance issuance,
            Instant occurredAt,
            Duration duration
    ) {
        return success(Operation.REFRESH, subject, issuance, occurredAt, duration);
    }

    /**
     * Creates a successful token revocation event.
     *
     * @param tokenKind  the revoked token kind
     * @param subject    the known subject, or {@code null}
     * @param occurredAt the completion time
     * @param duration   the elapsed operation duration
     * @return the immutable event
     */
    public static TokenLifecycleEvent revoked(
            TokenKind<?> tokenKind,
            @Nullable Subject subject,
            Instant occurredAt,
            Duration duration
    ) {
        TokenKind<?> nonNullTokenKind = Objects.requireNonNull(tokenKind, "tokenKind");
        return new TokenLifecycleEvent(
                Operation.REVOKE,
                Result.SUCCESS,
                Set.of(nonNullTokenKind.getName()),
                subject,
                null,
                occurredAt,
                duration
        );
    }

    /**
     * Creates a rejected token operation event.
     *
     * @param operation  the attempted token operation
     * @param subject    the known subject, or {@code null}
     * @param errorCode  the stable rejection code
     * @param occurredAt the completion time
     * @param duration   the elapsed operation duration
     * @return the immutable event
     */
    public static TokenLifecycleEvent rejected(
            Operation operation,
            @Nullable Subject subject,
            ErrorCode errorCode,
            Instant occurredAt,
            Duration duration
    ) {
        return new TokenLifecycleEvent(
                operation,
                Result.REJECTED,
                Set.of(),
                subject,
                Objects.requireNonNull(errorCode, "errorCode"),
                occurredAt,
                duration
        );
    }

    /**
     * Creates a failed token operation event.
     *
     * @param operation  the attempted token operation
     * @param subject    the known subject, or {@code null}
     * @param errorCode  the stable failure code
     * @param occurredAt the completion time
     * @param duration   the elapsed operation duration
     * @return the immutable event
     */
    public static TokenLifecycleEvent failed(
            Operation operation,
            @Nullable Subject subject,
            ErrorCode errorCode,
            Instant occurredAt,
            Duration duration
    ) {
        return new TokenLifecycleEvent(
                operation,
                Result.FAILED,
                Set.of(),
                subject,
                Objects.requireNonNull(errorCode, "errorCode"),
                occurredAt,
                duration
        );
    }

    /**
     * Returns the token operation.
     *
     * @return the operation
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Returns the operation result.
     *
     * @return the result
     */
    public Result getResult() {
        return result;
    }

    /**
     * Returns immutable canonical token kind names.
     *
     * @return the token kind names
     */
    public Set<String> getTokenKinds() {
        return tokenKinds;
    }

    /**
     * Returns the known subject.
     *
     * @return the subject, or {@code null}
     */
    public @Nullable Subject getSubject() {
        return subject;
    }

    /**
     * Returns the stable failure code.
     *
     * @return the error code, or {@code null} after a successful operation
     */
    public @Nullable ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the operation completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Returns the elapsed operation duration.
     *
     * @return the elapsed duration
     */
    public Duration getDuration() {
        return duration;
    }

    private static TokenLifecycleEvent success(
            Operation operation,
            Subject subject,
            TokenIssuance issuance,
            Instant occurredAt,
            Duration duration
    ) {
        TokenIssuance nonNullIssuance = Objects.requireNonNull(issuance, "issuance");
        Set<String> kinds = new LinkedHashSet<>();
        for (TokenKind<?> kind : nonNullIssuance.getKinds()) {
            kinds.add(Objects.requireNonNull(kind, "token kind").getName());
        }
        return new TokenLifecycleEvent(
                operation,
                Result.SUCCESS,
                kinds,
                Objects.requireNonNull(subject, "subject"),
                null,
                occurredAt,
                duration
        );
    }
}
