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

package com.lamprism.luxspec.security.firewall;

import com.lamprism.luxspec.ErrorCode;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of evaluating one firewall rule or complete rule chain.
 *
 * @author RollW
 */
public final class FirewallDecision {
    private static final FirewallDecision PASS = new FirewallDecision(true, null, null);
    private final boolean passed;
    private final @Nullable ErrorCode reasonCode;
    private final @Nullable Duration retryAfter;

    private FirewallDecision(boolean passed, @Nullable ErrorCode reasonCode, @Nullable Duration retryAfter) {
        this.passed = passed;
        this.reasonCode = reasonCode;
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the shared passing firewall decision.
     *
     * @return the passing decision
     */
    public static FirewallDecision pass() {
        return PASS;
    }

    /**
     * Creates a denial without a retry hint.
     *
     * @param reasonCode the stable denial reason
     * @return the denial decision
     */
    public static FirewallDecision deny(ErrorCode reasonCode) {
        return new FirewallDecision(false, Objects.requireNonNull(reasonCode, "reasonCode"), null);
    }

    /**
     * Creates a denial with one positive retry-after duration.
     *
     * @param reasonCode the stable denial reason
     * @param retryAfter the positive suggested retry delay
     * @return the denial decision
     */
    public static FirewallDecision deny(ErrorCode reasonCode, Duration retryAfter) {
        Duration nonNullRetryAfter = Objects.requireNonNull(retryAfter, "retryAfter");
        if (nonNullRetryAfter.isNegative() || nonNullRetryAfter.isZero()) {
            throw new IllegalArgumentException("retryAfter must be positive");
        }
        return new FirewallDecision(false, Objects.requireNonNull(reasonCode, "reasonCode"), nonNullRetryAfter);
    }

    /**
     * Reports whether the evaluated policy chain passed.
     *
     * @return {@code true} when all evaluated rules passed
     */
    public boolean passed() {
        return passed;
    }

    /**
     * Returns the stable denial reason.
     *
     * @return the denial reason
     * @throws IllegalStateException when this decision passed
     */
    public ErrorCode getReasonCode() {
        if (reasonCode == null) {
            throw new IllegalStateException("Passing decision has no reason code");
        }
        return reasonCode;
    }

    /**
     * Returns the optional retry hint for a denial.
     *
     * @return the optional retry duration
     */
    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
