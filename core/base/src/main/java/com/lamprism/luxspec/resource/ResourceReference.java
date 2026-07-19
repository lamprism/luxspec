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
     * @param id the required resource identifier
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
