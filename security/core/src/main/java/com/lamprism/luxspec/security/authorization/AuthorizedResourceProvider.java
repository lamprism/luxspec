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

import java.util.Collection;
import java.util.List;

/**
 * Provides resources only after the supplied action has been authorized.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface AuthorizedResourceProvider<ID> {
    /**
     * Creates an authorized provider with no-op decision publication.
     *
     * @param provider   the resource-loading provider
     * @param authorizer the instance-policy authorizer
     * @param <ID>       the resource ID type
     * @return the authorized provider
     */
    static <ID> AuthorizedResourceProvider<ID> of(
            ResourceProvider<ID> provider,
            ResourceAuthorizer<ID> authorizer
    ) {
        return new DefaultAuthorizedResourceProvider<>(
                provider,
                authorizer,
                ResourceAuthorizationPipeline.defaults()
        );
    }

    /**
     * Creates an authorized provider with decision event publication.
     *
     * @param provider       the resource-loading provider
     * @param authorizer     the instance-policy authorizer
     * @param eventPublisher the provider-independent event publisher
     * @param <ID>           the resource ID type
     * @return the authorized provider
     */
    static <ID> AuthorizedResourceProvider<ID> of(
            ResourceProvider<ID> provider,
            ResourceAuthorizer<ID> authorizer,
            EventPublisher eventPublisher
    ) {
        return new DefaultAuthorizedResourceProvider<>(
                provider,
                authorizer,
                ResourceAuthorizationPipeline.withEvents(eventPublisher)
        );
    }

    /**
     * Returns the resource type supported by this authorized provider.
     *
     * @return the resource type
     */
    ResourceType<ID> getResourceType();

    /**
     * Authorizes and loads one required resource.
     *
     * @param authentication the effective authenticated actor
     * @param action         the attempted resource action
     * @param reference      the required resource reference
     * @return the loaded resource
     */
    Resource<ID> provide(
            Authentication authentication,
            ResourceAction<ID> action,
            ResourceReference<ID> reference
    );

    /**
     * Authorizes every reference before loading the complete batch in input order.
     *
     * @param authentication the effective authenticated actor
     * @param action         the attempted resource action
     * @param references     the required resource references
     * @return loaded resources in input order
     */
    List<? extends Resource<ID>> provide(
            Authentication authentication,
            ResourceAction<ID> action,
            Collection<ResourceReference<ID>> references
    );
}
