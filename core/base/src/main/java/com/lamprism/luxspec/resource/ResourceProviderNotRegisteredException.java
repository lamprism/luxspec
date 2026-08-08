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
 * Indicates that no provider is registered for a requested resource type.
 *
 * @author RollW
 */
public final class ResourceProviderNotRegisteredException extends ResourceException {
    /**
     * Creates a missing-provider failure.
     *
     * @param resourceType the unregistered resource type
     */
    public ResourceProviderNotRegisteredException(ResourceType<?> resourceType) {
        super(ResourceErrorCode.PROVIDER_NOT_REGISTERED, message(resourceType));
    }

    private static String message(ResourceType<?> resourceType) {
        return "No ResourceProvider is registered for resource type: "
                + Objects.requireNonNull(resourceType, "resourceType").getName();
    }
}
