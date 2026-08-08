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

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.security.authentication.Authentication;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates one resource authorization decision inside the authorization package.
 *
 * @author RollW
 */
final class ResourceAuthorizationPipeline {
    private final EventPublisher eventPublisher;
    private final Clock clock;

    private ResourceAuthorizationPipeline(EventPublisher eventPublisher) {
        this(eventPublisher, Clock.systemUTC());
    }

    private ResourceAuthorizationPipeline(EventPublisher eventPublisher, Clock clock) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    static ResourceAuthorizationPipeline defaults() {
        return new ResourceAuthorizationPipeline(event -> {
        });
    }

    static ResourceAuthorizationPipeline withEvents(EventPublisher eventPublisher) {
        return new ResourceAuthorizationPipeline(eventPublisher);
    }

    AuthorizationDecision authorize(
            Authentication authentication,
            ResourceAction<?> action,
            ResourceReference<?> reference,
            ResourceAuthorizer<?> authorizer
    ) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ResourceAction<?> nonNullAction = Objects.requireNonNull(action, "action");
        ResourceReference<?> nonNullReference = Objects.requireNonNull(reference, "reference");
        ResourceAuthorizer<?> nonNullAuthorizer = Objects.requireNonNull(authorizer, "authorizer");
        Instant startedAt = clock.instant();
        requireMatchingTypes(nonNullAction, nonNullReference, nonNullAuthorizer);
        if (!nonNullAction.getRequirement().isSatisfiedBy(nonNullAuthentication.grants())) {
            return publishDecision(
                    nonNullAuthentication,
                    nonNullAction,
                    nonNullReference,
                    startedAt,
                    AuthorizationDecision.denied(AuthErrorCode.PERMISSION_DENIED)
            );
        }
        AuthorizationDecision decision = authorizeInstance(
                nonNullAuthentication,
                nonNullAction,
                nonNullReference,
                nonNullAuthorizer
        );
        return publishDecision(
                nonNullAuthentication,
                nonNullAction,
                nonNullReference,
                startedAt,
                decision
        );
    }

    private AuthorizationDecision authorizeInstance(
            Authentication authentication,
            ResourceAction<?> action,
            ResourceReference<?> reference,
            ResourceAuthorizer<?> authorizer
    ) {
        return Objects.requireNonNull(
                authorizeUnchecked(authentication, action, reference, authorizer),
                "authorization decision"
        );
    }

    @SuppressWarnings("unchecked")
    private AuthorizationDecision authorizeUnchecked(
            Authentication authentication,
            ResourceAction<?> action,
            ResourceReference<?> reference,
            ResourceAuthorizer<?> authorizer
    ) {
        return ((ResourceAuthorizer<Object>) authorizer).authorize(
                authentication,
                (ResourceAction<Object>) action,
                (ResourceReference<Object>) reference
        );
    }

    private AuthorizationDecision publishDecision(
            Authentication authentication,
            ResourceAction<?> action,
            ResourceReference<?> reference,
            Instant startedAt,
            AuthorizationDecision decision
    ) {
        Instant occurredAt = clock.instant();
        Duration elapsed = Duration.between(startedAt, occurredAt);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }
        publishEvent(authentication, action, reference, decision, occurredAt, elapsed);
        return decision;
    }

    @SuppressWarnings("unchecked")
    private void publishEvent(
            Authentication authentication,
            ResourceAction<?> action,
            ResourceReference<?> reference,
            AuthorizationDecision decision,
            Instant occurredAt,
            Duration elapsed
    ) {
        eventPublisher.publish(new ResourceAuthorizationDecisionEvent<>(
                authentication,
                (ResourceAction<Object>) action,
                (ResourceReference<Object>) reference,
                decision,
                occurredAt,
                elapsed
        ));
    }

    private void requireMatchingTypes(
            ResourceAction<?> action,
            ResourceReference<?> reference,
            ResourceAuthorizer<?> authorizer
    ) {
        if (!action.getResourceType().equals(reference.resourceType())) {
            throw new IllegalArgumentException("Resource action and reference types must match");
        }
        if (!action.getResourceType().equals(authorizer.getResourceType())) {
            throw new IllegalArgumentException("Resource action and authorizer types must match");
        }
    }
}
