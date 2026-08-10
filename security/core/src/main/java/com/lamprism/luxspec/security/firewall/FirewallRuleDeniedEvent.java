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
import com.lamprism.luxspec.event.Event;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Reports an ordinary firewall denial returned by a rule.
 *
 * <p>The event contains policy metadata only. It deliberately excludes the evaluated request,
 * client address, path, and any other request payload.</p>
 *
 * @author RollW
 */
public final class FirewallRuleDeniedEvent implements Event {
    private final String ruleType;
    private final ErrorCode reasonCode;
    private final @Nullable Duration retryAfter;
    private final Instant occurredAt;

    /**
     * Creates immutable denial metadata.
     *
     * @param ruleType   the denying rule implementation type
     * @param reasonCode the stable denial reason
     * @param retryAfter the optional positive retry hint
     * @param occurredAt the denial completion time
     */
    public FirewallRuleDeniedEvent(
            String ruleType,
            ErrorCode reasonCode,
            @Nullable Duration retryAfter,
            Instant occurredAt
    ) {
        this.ruleType = requireText(ruleType, "ruleType");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode");
        if (retryAfter != null && (retryAfter.isNegative() || retryAfter.isZero())) {
            throw new IllegalArgumentException("retryAfter must be positive");
        }
        this.retryAfter = retryAfter;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    /**
     * Returns the denying rule implementation type.
     *
     * @return the rule type
     */
    public String getRuleType() {
        return ruleType;
    }

    /**
     * Returns the stable denial reason.
     *
     * @return the denial reason
     */
    public ErrorCode getReasonCode() {
        return reasonCode;
    }

    /**
     * Returns the optional retry hint.
     *
     * @return the retry duration, or {@code null}
     */
    public @Nullable Duration getRetryAfter() {
        return retryAfter;
    }

    /**
     * Returns the denial completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
