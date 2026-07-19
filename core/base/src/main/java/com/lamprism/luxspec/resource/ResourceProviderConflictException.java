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
     * @param existingType the already registered type
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
