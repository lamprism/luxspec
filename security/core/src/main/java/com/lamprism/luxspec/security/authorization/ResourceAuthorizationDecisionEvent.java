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

package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.event.Event;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.security.authentication.Authentication;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Reports the final decision produced by one resource authorization attempt.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public final class ResourceAuthorizationDecisionEvent<ID> implements Event {
    private final Authentication authentication;
    private final ResourceAction<ID> action;
    private final ResourceReference<ID> reference;
    private final AuthorizationDecision decision;
    private final Instant occurredAt;
    private final Duration duration;

    /**
     * Creates an immutable authorization decision event without credentials or persistence state.
     *
     * @param authentication the effective authenticated actor
     * @param action         the attempted resource action
     * @param reference      the referenced resource
     * @param decision       the final authorization decision
     * @param occurredAt     the decision completion time
     * @param duration       the elapsed authorization duration
     */
    public ResourceAuthorizationDecisionEvent(
            Authentication authentication,
            ResourceAction<ID> action,
            ResourceReference<ID> reference,
            AuthorizationDecision decision,
            Instant occurredAt,
            Duration duration
    ) {
        this.authentication = Objects.requireNonNull(authentication, "authentication");
        this.action = Objects.requireNonNull(action, "action");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.decision = Objects.requireNonNull(decision, "decision");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        Duration nonNullDuration = Objects.requireNonNull(duration, "duration");
        if (nonNullDuration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        this.duration = nonNullDuration;
    }

    /**
     * Returns the effective authenticated actor.
     *
     * @return the authenticated actor
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * Returns the attempted resource action.
     *
     * @return the resource action
     */
    public ResourceAction<ID> getAction() {
        return action;
    }

    /**
     * Returns the referenced resource identity.
     *
     * @return the resource reference
     */
    public ResourceReference<ID> getReference() {
        return reference;
    }

    /**
     * Returns the final authorization decision.
     *
     * @return the authorization decision
     */
    public AuthorizationDecision getDecision() {
        return decision;
    }

    /**
     * Returns the decision completion time.
     *
     * @return the completion time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Returns the elapsed authorization duration.
     *
     * @return the elapsed duration
     */
    public Duration getDuration() {
        return duration;
    }
}
