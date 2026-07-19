package com.lamprism.luxspec.user;

import com.lamprism.luxspec.resource.ResourceType;

/**
 * Holds user resource identities.
 *
 * @author RollW
 */
public final class UserResourceTypes {
    public static final ResourceType<Long> USER = ResourceType.of("USER", Long.class);

    private UserResourceTypes() {
    }
}
