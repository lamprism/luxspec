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

import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;

/**
 * Evaluates resource-instance policy after an action's baseline grant requirement passes.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface ResourceAuthorizer<ID> {
    /**
     * Returns the resource type this authorizer owns.
     *
     * @return the owned resource type
     */
    ResourceType<ID> getResourceType();

    /**
     * Applies instance-level policy after baseline requirements have passed.
     *
     * @param authentication the effective authenticated actor
     * @param action         the attempted resource action
     * @param reference      the referenced resource
     * @return the final instance-level decision
     */
    AuthorizationDecision authorize(
            Authentication authentication,
            ResourceAction<ID> action,
            ResourceReference<ID> reference
    );
}
