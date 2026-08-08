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

package com.lamprism.luxspec.resource;

import java.util.Collection;
import java.util.List;

/**
 * Resolves loaded resources from typed references.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface ResourceProvider<ID> {
    /**
     * Returns the one resource type handled by this provider.
     *
     * @return the handled resource type
     */
    ResourceType<ID> getResourceType();

    /**
     * Resolves one required typed resource reference.
     *
     * @param reference the reference to resolve
     * @return the resolved non-null resource
     */
    Resource<ID> provide(ResourceReference<ID> reference);

    /**
     * Resolves all required references in input order.
     *
     * @param references the references to resolve
     * @return immutable resolved resources preserving input order and duplicate positions
     */
    List<? extends Resource<ID>> provide(Collection<ResourceReference<ID>> references);
}
