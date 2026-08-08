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

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.resource.Resource;
import com.lamprism.luxspec.resource.ResourceBrowser;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default authorized resource browser implementation.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public class DefaultAuthorizedResourceBrowser<ID> implements AuthorizedResourceBrowser<ID> {
    private final ResourceBrowser<ID> browser;
    private final ResourceAuthorizer<ID> authorizer;
    private final ResourceAuthorizationPipeline pipeline;

    /**
     * Creates a default authorized browser without decision event publication.
     *
     * @param browser    the domain browser that owns query visibility and pagination
     * @param authorizer the instance-policy authorizer
     */
    public DefaultAuthorizedResourceBrowser(
            ResourceBrowser<ID> browser,
            ResourceAuthorizer<ID> authorizer
    ) {
        this(browser, authorizer, ResourceAuthorizationPipeline.defaults());
    }

    /**
     * Creates a default authorized browser with decision event publication.
     *
     * @param browser        the domain browser that owns query visibility and pagination
     * @param authorizer     the instance-policy authorizer
     * @param eventPublisher the provider-independent event publisher
     */
    public DefaultAuthorizedResourceBrowser(
            ResourceBrowser<ID> browser,
            ResourceAuthorizer<ID> authorizer,
            EventPublisher eventPublisher
    ) {
        this(browser, authorizer, ResourceAuthorizationPipeline.withEvents(eventPublisher));
    }

    DefaultAuthorizedResourceBrowser(
            ResourceBrowser<ID> browser,
            ResourceAuthorizer<ID> authorizer,
            ResourceAuthorizationPipeline pipeline
    ) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.authorizer = Objects.requireNonNull(authorizer, "authorizer");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        if (!browser.getResourceType().equals(authorizer.getResourceType())) {
            throw new IllegalArgumentException("Resource browser and authorizer types must match");
        }
    }

    @Override
    public ResourceType<ID> getResourceType() {
        return browser.getResourceType();
    }

    @Override
    public QueryResult<? extends Resource<ID>> browse(
            Authentication authentication,
            ResourceAction<ID> action,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ResourceAction<ID> nonNullAction = Objects.requireNonNull(action, "action");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        QueryResult<? extends Resource<ID>> result = Objects.requireNonNull(
                browser.browse(nonNullCriteria, nonNullWindow),
                "result"
        );
        List<ResourceReference<ID>> references = referencesOf(result);
        for (ResourceReference<ID> reference : references) {
            AuthorizationDecision decision = pipeline.authorize(
                    nonNullAuthentication,
                    nonNullAction,
                    reference,
                    authorizer
            );
            requireAllowed(decision);
        }
        return result;
    }

    private List<ResourceReference<ID>> referencesOf(QueryResult<? extends Resource<ID>> result) {
        List<? extends Resource<ID>> items = Objects.requireNonNull(result.getItems(), "result.items");
        List<ResourceReference<ID>> references = new ArrayList<>(items.size());
        for (Resource<ID> item : items) {
            Resource<ID> nonNullItem = Objects.requireNonNull(item, "result item");
            references.add(Objects.requireNonNull(nonNullItem.getReference(), "resource reference"));
        }
        return List.copyOf(references);
    }

    private void requireAllowed(AuthorizationDecision decision) {
        AuthorizationDecision nonNullDecision = Objects.requireNonNull(decision, "authorization decision");
        if (!nonNullDecision.isAllowed()) {
            throw new ResourceAccessDeniedException(nonNullDecision.getReasonCode());
        }
    }
}
