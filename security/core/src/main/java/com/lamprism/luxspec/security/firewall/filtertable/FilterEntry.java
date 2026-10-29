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
 * @author RollW
 */
public final class FilterEntry {
    private final FilterTarget target;
    private final FilterMode mode;
    private final @Nullable Instant expiresAt;
    private final @Nullable String reason;
    /**
     * Creates one validated filter entry.
     *
     * @param target    the request fact matched by the entry
     * @param mode      the action for a matching request
     * @param expiresAt the exclusive expiration instant, or {@code null} for no expiration
     * @param reason    optional operator-provided reason
     */
    public FilterEntry(FilterTarget target, FilterMode mode, @Nullable Instant expiresAt, @Nullable String reason) {
        this.target = Objects.requireNonNull(target, "target");
        this.mode = Objects.requireNonNull(mode, "mode");
        if (reason != null && reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    /**
     * Returns the request fact matched by this entry.
     *
     * @return the target
     */
    public FilterTarget target() {
        return target;
    }

    /**
     * Returns the action for a matching request.
     *
     * @return the mode
     */
    public FilterMode mode() {
        return mode;
    }

    /**
     * Returns the exclusive expiration time, if configured.
     *
     * @return the expiration time, or {@code null}
     */
    public @Nullable Instant expiresAt() {
        return expiresAt;
    }

    /**
     * Returns the optional operator-provided reason.
     *
     * @return the reason, or {@code null}
     */
    public @Nullable String reason() {
        return reason;
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

    @Override
    public boolean equals(Object other) {
        return other instanceof FilterEntry entry
                && target.equals(entry.target)
                && mode == entry.mode
                && Objects.equals(expiresAt, entry.expiresAt)
                && Objects.equals(reason, entry.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, mode, expiresAt, reason);
    }
}
