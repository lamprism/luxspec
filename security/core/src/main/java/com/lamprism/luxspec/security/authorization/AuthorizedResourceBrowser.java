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
import com.lamprism.luxspec.resource.Resource;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;

/**
 * Browses resources whose visibility is enforced before counting, ordering, and pagination.
 *
 * <p>Implementations must apply the authenticated actor and action as query-visible restrictions.
 * They must not page an unrestricted result and authorize the returned items afterward.</p>
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface AuthorizedResourceBrowser<ID> {
    /**
     * Returns the resource type supported by this authorized browser.
     *
     * @return the resource type
     */
    ResourceType<ID> getResourceType();

    /**
     * Browses resources visible to the supplied actor for the supplied action.
     *
     * @param authentication the effective authenticated actor
     * @param action         the attempted resource action
     * @param criteria       the validated structured query criteria
     * @param window         the requested result window
     * @return the query result produced from the authorized visibility restriction
     */
    QueryResult<? extends Resource<ID>> browse(
            Authentication authentication,
            ResourceAction<ID> action,
            QueryCriteria criteria,
            QueryWindow window
    );
}
