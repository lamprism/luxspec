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
 * Represents the result of evaluating one firewall or a complete firewall registry.
 *
 * @author RollW
 */
public final class FirewallDecision {
    /**
     * Describes how a registry should handle the evaluated request.
     */
    public enum Case {
        /**
         * Stop evaluation and allow the request.
         */
        ALLOW,
        /**
         * Continue evaluation with the next registered firewall.
         */
        NEUTRAL,
        /**
         * Stop evaluation and deny the request.
         */
        DENY
    }

    private static final FirewallDecision ALLOW = new FirewallDecision(Case.ALLOW, null, null);
    private static final FirewallDecision NEUTRAL = new FirewallDecision(Case.NEUTRAL, null, null);
    private final Case decisionCase;
    private final @Nullable ErrorCode reasonCode;
    private final @Nullable Duration retryAfter;

    private FirewallDecision(Case decisionCase, @Nullable ErrorCode reasonCode, @Nullable Duration retryAfter) {
        this.decisionCase = Objects.requireNonNull(decisionCase, "decisionCase");
        this.reasonCode = reasonCode;
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the shared explicit allow decision.
     *
     * @return the allow decision
     */
    public static FirewallDecision allow() {
        return ALLOW;
    }

    /**
     * Returns the shared neutral decision.
     *
     * @return the neutral decision
     */
    public static FirewallDecision neutral() {
        return NEUTRAL;
    }

    /**
     * Creates a denial without a retry hint.
     *
     * @param reasonCode the stable denial reason
     * @return the denial decision
     */
    public static FirewallDecision deny(ErrorCode reasonCode) {
        return new FirewallDecision(Case.DENY, Objects.requireNonNull(reasonCode, "reasonCode"), null);
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
        return new FirewallDecision(Case.DENY, Objects.requireNonNull(reasonCode, "reasonCode"), nonNullRetryAfter);
    }

    /**
     * Returns the three-state registry decision.
     *
     * @return the decision case
     */
    public Case getCase() {
        return decisionCase;
    }

    /**
     * Reports whether this decision does not deny the request.
     *
     * @return {@code true} for {@link Case#ALLOW} and {@link Case#NEUTRAL}
     */
    public boolean passed() {
        return decisionCase != Case.DENY;
    }

    /**
     * Returns the stable denial reason.
     *
     * @return the denial reason
     * @throws IllegalStateException when this decision is not a denial
     */
    public ErrorCode getReasonCode() {
        if (decisionCase != Case.DENY || reasonCode == null) {
            throw new IllegalStateException("Non-denial decision has no reason code");
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
