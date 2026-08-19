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
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.resource.InMemoryResourceBrowser;
import com.lamprism.luxspec.resource.Resource;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;

import java.util.Objects;

/**
 * An authorized browser that applies instance visibility to an in-memory resource snapshot.
 *
 * <p>This implementation is intended for tests, local applications, and small datasets. It applies
 * structured criteria before instance visibility, then applies visibility before ordering, counting,
 * and windowing. It does not publish one authorization event per scanned resource because a browse
 * operation has no single requested reference. Persistent and remote implementations must apply
 * equivalent visibility in their native query instead.</p>
 *
 * @param <ID> the resource ID type
 * @param <R>  the loaded resource type
 * @author RollW
 */
public class InMemoryAuthorizedResourceBrowser<ID, R extends Resource<ID>>
        implements AuthorizedResourceBrowser<ID> {
    private final InMemoryResourceBrowser<ID, R> browser;
    private final ResourceAuthorizer<ID> authorizer;

    /**
     * Creates an authorized browser over one compatible in-memory resource browser.
     *
     * @param browser    the snapshot-based resource browser
     * @param authorizer the instance-level authorization policy
     */
    public InMemoryAuthorizedResourceBrowser(
            InMemoryResourceBrowser<ID, R> browser,
            ResourceAuthorizer<ID> authorizer
    ) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.authorizer = Objects.requireNonNull(authorizer, "authorizer");
        if (!browser.getResourceType().equals(authorizer.getResourceType())) {
            throw new IllegalArgumentException("Resource browser and authorizer types must match");
        }
    }

    /**
     * Returns the resource type handled by the underlying browser.
     *
     * @return the handled resource type
     */
    @Override
    public ResourceType<ID> getResourceType() {
        return browser.getResourceType();
    }

    /**
     * Browses only resources visible to the authenticated actor.
     *
     * @param authentication the effective authenticated actor
     * @param action         the attempted resource action
     * @param criteria       the validated structured query criteria
     * @param window         the requested result window
     * @return the query result calculated from visible resources only
     */
    @Override
    public QueryResult<R> browse(
            Authentication authentication,
            ResourceAction<ID> action,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ResourceAction<ID> nonNullAction = Objects.requireNonNull(action, "action");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        requireMatchingAction(nonNullAction);
        if (!nonNullAction.getRequirement().isSatisfiedBy(nonNullAuthentication.grants())) {
            throw new ResourceAccessDeniedException(AuthErrorCode.PERMISSION_DENIED);
        }
        return browser.browse(
                resource -> isVisible(nonNullAuthentication, nonNullAction, resource),
                nonNullCriteria,
                nonNullWindow
        );
    }

    private void requireMatchingAction(ResourceAction<ID> action) {
        if (!browser.getResourceType().equals(action.getResourceType())) {
            throw new IllegalArgumentException("Resource browser and action types must match");
        }
    }

    private boolean isVisible(Authentication authentication, ResourceAction<ID> action, R resource) {
        AuthorizationDecision decision = Objects.requireNonNull(
                authorizer.authorize(authentication, action, resource.getReference()),
                "authorization decision"
        );
        return decision.isAllowed();
    }
}
