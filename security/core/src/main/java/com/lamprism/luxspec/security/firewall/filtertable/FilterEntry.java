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

package com.lamprism.luxspec.security.firewall.filtertable;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable filter-table entry.
 *
 * @param target    the request fact matched by the entry
 * @param mode      the action for a matching request
 * @param expiresAt the exclusive expiration instant, or {@code null} for no expiration
 * @param reason    optional operator-provided reason
 * @author RollW
 */
public record FilterEntry(
        FilterTarget target,
        FilterMode mode,
        @Nullable Instant expiresAt,
        @Nullable String reason
) {
    /**
     * Creates one validated filter entry.
     *
     * @param target    the request fact matched by the entry
     * @param mode      the action for a matching request
     * @param expiresAt the exclusive expiration instant, or {@code null} for no expiration
     * @param reason    optional operator-provided reason
     */
    public FilterEntry {
        target = Objects.requireNonNull(target, "target");
        mode = Objects.requireNonNull(mode, "mode");
        if (reason != null && reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }

    /**
     * Creates a non-expiring allow entry.
     *
     * @param target the target to allow
     * @return the allow entry
     */
    public static FilterEntry allow(FilterTarget target) {
        return new FilterEntry(target, FilterMode.ALLOW, null, null);
    }

    /**
     * Creates a non-expiring deny entry.
     *
     * @param target the target to deny
     * @return the deny entry
     */
    public static FilterEntry deny(FilterTarget target) {
        return new FilterEntry(target, FilterMode.DENY, null, null);
    }

    /**
     * Reports whether this entry is active at the supplied instant.
     *
     * @param instant the expiration evaluation instant
     * @return {@code true} when the entry is active
     */
    public boolean isActiveAt(Instant instant) {
        Instant nonNullInstant = Objects.requireNonNull(instant, "instant");
        return expiresAt == null || expiresAt.isAfter(nonNullInstant);
    }
}
