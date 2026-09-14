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
 * Indicates conflicting provider registrations for one normalized resource type name.
 *
 * @author RollW
 */
public final class ResourceProviderConflictException extends ResourceException {
    /**
     * Creates a conflict between the existing and requested type registrations.
     *
     * @param existingType  the already registered type
     * @param requestedType the conflicting requested type
     */
    public ResourceProviderConflictException(ResourceType<?> existingType, ResourceType<?> requestedType) {
        super(ResourceErrorCode.PROVIDER_CONFLICT, message(existingType, requestedType));
    }

    private static String message(ResourceType<?> existingType, ResourceType<?> requestedType) {
        ResourceType<?> nonNullExistingType = Objects.requireNonNull(existingType, "existingType");
        ResourceType<?> nonNullRequestedType = Objects.requireNonNull(requestedType, "requestedType");
        if (nonNullExistingType.equals(nonNullRequestedType)) {
            return "A ResourceProvider is already registered for resource type: " + nonNullExistingType.getName();
        }
        return "ResourceProvider ID types conflict for resource type: " + nonNullExistingType.getName();
    }
}
