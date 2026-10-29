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

package com.lamprism.luxspec.audit.retention;

import com.lamprism.luxspec.audit.AuditEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Selects audit entries eligible for removal at an explicit time.
 *
 * <p>Policies must be thread-safe, nonblocking, and free of side effects. They may retain
 * individual entries indefinitely or apply different rules by entry.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuditRetentionPolicy {
    /**
     * Retains entries for a fixed duration after their publication time.
     *
     * @param duration the positive retention duration
     * @return the retention policy
     */
    static AuditRetentionPolicy afterPublication(Duration duration) {
        Duration retention = Objects.requireNonNull(duration, "duration");
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("Retention duration must be positive");
        }
        return (entry, now) -> !entry.publishedAt().plus(retention).isAfter(now);
    }

    /**
     * Determines whether an entry may be removed.
     *
     * @param entry the immutable audit entry
     * @param now   the retention evaluation instant
     * @return whether the entry is eligible for removal
     */
    boolean shouldRemove(AuditEntry entry, Instant now);
}
