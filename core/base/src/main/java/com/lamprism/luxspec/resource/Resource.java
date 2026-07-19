package com.lamprism.luxspec.resource;


/**
 * A loaded immutable public resource model.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface Resource<ID> {
    /**
     * Returns the loaded resource's typed identity.
     *
     * @return the resource reference
     */
    ResourceReference<ID> getReference();
}
