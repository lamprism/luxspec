package com.lamprism.luxspec.user.security;

import com.lamprism.luxspec.user.Role;
import java.util.Map;
import java.util.Set;

/**
 * Defines the stable profiles attached to predefined generic user roles.
 *
 * @author RollW
 */
public final class UserAuthorizationProfiles {
    /**
     * Contains scopes granted to ordinary users.
     */
    public static final AuthorizationProfile USER = AuthorizationProfile.of("user");
    /**
     * Contains scopes granted only to administrators in addition to user scopes.
     */
    public static final AuthorizationProfile ADMIN = AuthorizationProfile.of("admin");

    private UserAuthorizationProfiles() {
    }

    /**
     * Returns the default Role-to-Profile mapping without listing concrete feature scopes.
     *
     * @return immutable predefined role profile mappings
     */
    public static Map<Role, Set<AuthorizationProfile>> defaultRoleProfiles() {
        return Map.of(
                Role.USER, Set.of(USER),
                Role.ADMIN, Set.of(ADMIN)
        );
    }

    /**
     * Creates the default profile inheritance contribution.
     *
     * @return the default profile contributor
     */
    public static AuthorizationProfileContributor defaults() {
        return registry -> {
            registry.register(USER);
            registry.register(ADMIN);
            registry.inherit(ADMIN, USER);
        };
    }
}
