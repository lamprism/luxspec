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

import java.util.Objects;

/**
 * Identifies one unloaded resource.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public final class ResourceReference<ID> {
    private final ResourceType<ID> resourceType;
    private final ID id;

    /**
     * Creates a typed unloaded resource identity.
     *
     * @param resourceType the resource category
     * @param id           the required resource identifier
     */
    public ResourceReference(ResourceType<ID> resourceType, ID id) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.id = Objects.requireNonNull(id, "id");
        if (!resourceType.getIdType().isInstance(id)) {
            throw new IllegalArgumentException("Resource ID does not match resource type");
        }
    }

    /**
     * Returns the referenced resource category.
     *
     * @return the resource type
     */
    public ResourceType<ID> resourceType() {
        return resourceType;
    }

    /**
     * Returns the typed resource identifier.
     *
     * @return the resource ID
     */
    public ID id() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ResourceReference<?> reference)) {
            return false;
        }
        return resourceType.equals(reference.resourceType) && id.equals(reference.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, id);
    }

    @Override
    public String toString() {
        return resourceType.getName() + ":" + id;
    }
}
