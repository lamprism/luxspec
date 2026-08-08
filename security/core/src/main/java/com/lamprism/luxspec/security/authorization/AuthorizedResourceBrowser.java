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
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;

/**
 * Browses resources only after the supplied action has been authorized for every result item.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface AuthorizedResourceBrowser<ID> {
    /**
     * Creates an authorized browser with no-op decision publication.
     *
     * @param browser    the domain browser that owns query visibility and pagination
     * @param authorizer the instance-policy authorizer
     * @param <ID>       the resource ID type
     * @return the authorized browser
     */
    static <ID> AuthorizedResourceBrowser<ID> of(
            ResourceBrowser<ID> browser,
            ResourceAuthorizer<ID> authorizer
    ) {
        return new DefaultAuthorizedResourceBrowser<>(
                browser,
                authorizer,
                ResourceAuthorizationPipeline.defaults()
        );
    }

    /**
     * Creates an authorized browser with decision event publication.
     *
     * @param browser        the domain browser that owns query visibility and pagination
     * @param authorizer     the instance-policy authorizer
     * @param eventPublisher the provider-independent event publisher
     * @param <ID>           the resource ID type
     * @return the authorized browser
     */
    static <ID> AuthorizedResourceBrowser<ID> of(
            ResourceBrowser<ID> browser,
            ResourceAuthorizer<ID> authorizer,
            EventPublisher eventPublisher
    ) {
        return new DefaultAuthorizedResourceBrowser<>(
                browser,
                authorizer,
                ResourceAuthorizationPipeline.withEvents(eventPublisher)
        );
    }

    /**
     * Returns the resource type supported by this authorized browser.
     *
     * @return the resource type
     */
    ResourceType<ID> getResourceType();

    /**
     * Browses visible resources and requires every returned item to be authorized.
     *
     * @param authentication the effective authenticated actor
     * @param action         the attempted resource action
     * @param criteria       the validated structured query criteria
     * @param window         the requested result window
     * @return the unchanged authorized query result
     */
    QueryResult<? extends Resource<ID>> browse(
            Authentication authentication,
            ResourceAction<ID> action,
            QueryCriteria criteria,
            QueryWindow window
    );
}
