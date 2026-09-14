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

import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.resource.Resource;
import com.lamprism.luxspec.resource.ResourceProvider;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Default authorized resource provider implementation.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public class DefaultAuthorizedResourceProvider<ID> implements AuthorizedResourceProvider<ID> {
    private final ResourceProvider<ID> provider;
    private final ResourceAuthorizer<ID> authorizer;
    private final ResourceAuthorizationPipeline pipeline;

    /**
     * Creates a default authorized provider without decision event publication.
     *
     * @param provider   the resource-loading provider
     * @param authorizer the instance-policy authorizer
     */
    public DefaultAuthorizedResourceProvider(
            ResourceProvider<ID> provider,
            ResourceAuthorizer<ID> authorizer
    ) {
        this(provider, authorizer, ResourceAuthorizationPipeline.defaults());
    }

    /**
     * Creates a default authorized provider with decision event publication.
     *
     * @param provider       the resource-loading provider
     * @param authorizer     the instance-policy authorizer
     * @param eventPublisher the provider-independent event publisher
     */
    public DefaultAuthorizedResourceProvider(
            ResourceProvider<ID> provider,
            ResourceAuthorizer<ID> authorizer,
            EventPublisher eventPublisher
    ) {
        this(provider, authorizer, ResourceAuthorizationPipeline.withEvents(eventPublisher));
    }

    DefaultAuthorizedResourceProvider(
            ResourceProvider<ID> provider,
            ResourceAuthorizer<ID> authorizer,
            ResourceAuthorizationPipeline pipeline
    ) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.authorizer = Objects.requireNonNull(authorizer, "authorizer");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        if (!provider.getResourceType().equals(authorizer.getResourceType())) {
            throw new IllegalArgumentException("Resource provider and authorizer types must match");
        }
    }

    @Override
    public ResourceType<ID> getResourceType() {
        return provider.getResourceType();
    }

    @Override
    public Resource<ID> provide(
            Authentication authentication,
            ResourceAction<ID> action,
            ResourceReference<ID> reference
    ) {
        AuthorizationDecision decision = pipeline.authorize(authentication, action, reference, authorizer);
        requireAllowed(decision);
        return provider.provide(reference);
    }

    @Override
    public List<? extends Resource<ID>> provide(
            Authentication authentication,
            ResourceAction<ID> action,
            Collection<ResourceReference<ID>> references
    ) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ResourceAction<ID> nonNullAction = Objects.requireNonNull(action, "action");
        List<ResourceReference<ID>> requestedReferences = List.copyOf(
                Objects.requireNonNull(references, "references")
        );
        List<AuthorizationDecision> decisions = new ArrayList<>(requestedReferences.size());
        for (ResourceReference<ID> reference : requestedReferences) {
            decisions.add(pipeline.authorize(nonNullAuthentication, nonNullAction, reference, authorizer));
        }
        for (AuthorizationDecision decision : decisions) {
            requireAllowed(decision);
        }
        return provider.provide(requestedReferences);
    }

    private void requireAllowed(AuthorizationDecision decision) {
        AuthorizationDecision nonNullDecision = Objects.requireNonNull(decision, "authorization decision");
        if (!nonNullDecision.isAllowed()) {
            throw new ResourceAccessDeniedException(nonNullDecision.getReasonCode());
        }
    }
}
