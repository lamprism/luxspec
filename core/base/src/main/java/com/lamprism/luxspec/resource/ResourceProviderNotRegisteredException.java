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
