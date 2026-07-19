package com.lamprism.luxspec.user;

import com.lamprism.luxspec.resource.ResourceProvider;
import com.lamprism.luxspec.resource.ResourceReference;
import java.util.Collection;
import java.util.List;

/**
 * Resolves users by their resource references and unique usernames.
 *
 * @author RollW
 */
public interface UserProvider extends ResourceProvider<Long> {
    /**
     * Resolves one required user reference.
     *
     * @param reference the user reference
     * @return the resolved user
     */
    @Override
    User provide(ResourceReference<Long> reference);

    /**
     * Resolves all required user references while preserving input order.
     *
     * @param references the user references
     * @return the resolved users in input order
     */
    @Override
    List<User> provide(Collection<ResourceReference<Long>> references);

    /**
     * Resolves one required user by its unique username.
     *
     * @param username the username
     * @return the resolved user
     */
    User provideByUsername(String username);
}
