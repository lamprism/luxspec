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
